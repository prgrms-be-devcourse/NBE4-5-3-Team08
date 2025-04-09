package com.team8.project2.domain.link.service

import com.team8.project2.domain.link.dto.LinkClickResDto
import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.repository.LinkRepository
import com.team8.project2.global.exception.ServiceException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration


/**
 * 링크(Link) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 링크 추가, 수정, 삭제 및 조회 기능을 제공합니다.
 */
@Service
class LinkService(
    private val linkRepository: LinkRepository,
    private val linkClickService: LinkClickService,
    private val redisTemplate: RedisTemplate<String, String> // RedisTemplate 추가
) {
    companion object {
        private const val CLICK_KEY = "link:click:" // Redis 키 접두사
    }
    /**
     * 특정 링크를 조회하고 클릭수를 증가시킵니다.
     *
     * @param linkId 조회할 링크 ID
     * @param request 클라이언트 요청 객체
     * @return 클릭된 링크 객체
     */
    @Transactional
    fun getLinkAndIncrementClick(linkId: Long, request: HttpServletRequest): LinkClickResDto {
        var ip = listOf(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP",
            "X-RealIP",
            "REMOTE_ADDR"
        ).firstNotNullOfOrNull {
            request.getHeader(it).takeUnless { header -> header.isNullOrBlank() || header.equals("unknown", true) }
        } ?: request.remoteAddr

        if (ip == "0:0:0:0:0:0:0:1" || ip == "127.0.0.1") {
            try {
                val address = InetAddress.getLocalHost()
                ip = "${address.hostName}/${address.hostAddress}"
            } catch (e: UnknownHostException) {
                throw RuntimeException(e)
            }
        }

        val key = "$CLICK_KEY$linkId:$ip"
        val isNewClick = redisTemplate.opsForValue().setIfAbsent(key, "true", Duration.ofMinutes(10)) == true
        println("Redis Key Set? $isNewClick | Key: $key")

        val link = linkRepository.findById(linkId)
            .orElseThrow { ServiceException("404-1", "해당 링크를 찾을 수 없습니다.") }

        if (isNewClick) {
            linkClickService.increaseClickCount(link)
            println("클릭수 증가! 현재 조회수: ${link.click}")
        } else {
            println("클릭수 증가 안 함 (이미 조회된 IP)")
        }

        return LinkClickResDto.fromEntity(link)
    }


    /**
     * 새로운 링크를 추가합니다.
     *
     * @param linkReqDTO 링크 추가 요청 데이터 객체
     * @return 생성된 링크 객체
     */
    @Transactional
    fun addLink(linkReqDTO: LinkReqDTO): Link {
        val link = Link.builder()
            .title(linkReqDTO.title.orEmpty()) // nullable → non-null
            .url(linkReqDTO.url!!)              // NotNull 어노테이션 기반 강제 non-null
            .description(linkReqDTO.description.orEmpty())
            .click(0)
            .build()
        return linkRepository.save(link)
    }


    /**
     * 기존 링크를 수정합니다.
     *
     * @param linkId 수정할 링크 ID
     * @param url    새로운 링크 URL
     * @return 수정된 링크 객체
     */
    @Transactional
    fun updateLink(linkId: Long, url: String?): Link {
        val link = linkRepository.findById(linkId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 링크를 찾을 수 없습니다."
                )
            }
        link.url = url
        return linkRepository.save(link)
    }

    /**
     * 특정 링크를 삭제합니다.
     *
     * @param linkId 삭제할 링크 ID
     */
    @Transactional
    fun deleteLink(linkId: Long) {
        val link = linkRepository.findById(linkId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 링크를 찾을 수 없습니다."
                )
            }
        linkRepository.delete(link)
    }

    /**
     * 링크가 존재하면 기존 링크를 반환하고, 존재하지 않으면 새로 생성하여 반환합니다.
     *
     * @param url 조회할 링크 URL
     * @return 기존 또는 새로 생성된 링크 객체
     */
    @Transactional
    fun getLink(url: String?): Link {
        val actualUrl = url ?: throw ServiceException("400", "URL은 필수입니다.")

        linkRepository.findByUrl(actualUrl)?.let { return it }


        val link = Link.builder()
            .url(actualUrl)
            .build()
        link.loadMetadata()
        return linkRepository.save(link)
    }



    /**
     * 링크의 제목, URL, 설명을 수정합니다.
     *
     * @param linkId      수정할 링크 ID
     * @param title       새로운 링크 제목
     * @param url         새로운 링크 URL
     * @param description 새로운 링크 설명
     * @return 수정된 링크 객체
     */
    @Transactional
    fun updateLinkDetails(linkId: Long, title: String?, url: String?, description: String?): Link {
        val link = linkRepository.findById(linkId)
            .orElseThrow { ServiceException("404", "해당 링크를 찾을 수 없습니다.") }

        link.title = title.orEmpty()
        link.url = url ?: throw ServiceException("400", "url은 필수입니다.")
        link.description = description.orEmpty()

        return linkRepository.save(link)
    }


}
