package com.team8.project2.domain.curation.curation.service

import CurationDetailResDto
import com.team8.project2.domain.curation.curation.dto.CurationResDto
import com.team8.project2.domain.curation.curation.dto.CurationSearchResDto
import com.team8.project2.domain.curation.curation.dto.TrendingCurationResDto
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.entity.CurationLink
import com.team8.project2.domain.curation.curation.entity.CurationTag
import com.team8.project2.domain.curation.curation.entity.SearchOrder
import com.team8.project2.domain.curation.curation.event.CurationDeleteEvent
import com.team8.project2.domain.curation.curation.event.CurationUpdateEvent
import com.team8.project2.domain.curation.curation.repository.CurationLinkRepository
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.repository.CurationTagRepository
import com.team8.project2.domain.curation.like.entity.Like
import com.team8.project2.domain.curation.like.repository.LikeRepository
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.curation.tag.service.TagService
import com.team8.project2.domain.image.repository.CurationImageRepository
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.ServiceException
import jakarta.servlet.http.HttpServletRequest
import lombok.RequiredArgsConstructor
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.util.stream.Collectors

/**
 * 큐레이션 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 큐레이션 생성, 수정, 삭제, 조회 및 좋아요 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
class CurationService(
    private val curationRepository: CurationRepository,
    private val curationLinkRepository: CurationLinkRepository,
    private val curationTagRepository: CurationTagRepository,
    private val curationImageRepository: CurationImageRepository,
    private val linkService: LinkService,
    private val tagService: TagService,
    private val memberRepository: MemberRepository,
    private val likeRepository: LikeRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val curationViewService: CurationViewService,
    private val rq: Rq,
    private val redisTemplate: RedisTemplate<String, String>,
    private val memberService: MemberService,
    private val reportRepository: ReportRepository,
) {

    /**
     * ✅ 특정 큐레이터의 큐레이션 개수를 반환하는 메서드 추가
     * @param member 조회할 큐레이터의 memberId
     * @return 해당 큐레이터가 작성한 큐레이션 개수
     */
    @Transactional
    fun countByMember(member: Member): Long {
        return curationRepository.countByMemberId(member.getMemberId())
    }

    /**
     * 큐레이션을 생성합니다.
     * @param title 큐레이션 제목
     * @param content 큐레이션 내용
     * @param urls 연결된 링크 목록
     * @param tags 연결된 태그 목록
     * @return 생성된 큐레이션 객체
     */
    @Transactional
    fun createCuration(
        title: String,
        content: String,
        urls: List<String>,
        tags: List<String>,
        member: Member
    ): Curation {
        val curation = Curation(
            title = title,
            content = content,
            member = member
        )
        curationRepository.save(curation)
        // 큐레이션 - 링크 연결
        val curationLinks = urls
            .map { url ->
                val curationLink = CurationLink()
                curationLink.setCurationAndLink(curation, linkService.getLink(url))
            }
        curationLinkRepository.saveAll(curationLinks)
        curation.curationLinks.apply {
            clear()
            addAll(curationLinks)
        }


        // 큐레이션 - 태그 연결
        val curationTags = tags
            .map { tag ->
                val curationTag = CurationTag()
                curationTag.setCurationAndTag(curation, tagService.getTag(tag))
            }
        curationTagRepository.saveAll(curationTags)
        curation.tags.apply {
            clear()
            addAll(curationTags)
        }

        // 작성한 큐레이션에 이미지가 첨부되어 있다면, 이미지에 큐레이션 번호를 연결 (연결이 이미 있는 이미지는 무시)
        val imageNames = curation.imageNames

        for (i in imageNames.indices) {
            println(i.toString() + ": " + imageNames[i])
        }

        for (imageName in imageNames) {
            val opImage = curationImageRepository.findByImageName(imageName)
            if (opImage != null) {
                val curationImage = opImage
                curationImage.setCurationIdIfNull(curation.id!!)
                curationImageRepository.save(curationImage)
            }
        }

        return curation
    }

    /**
     * 큐레이션을 수정합니다.
     * @param curationId 수정할 큐레이션 ID
     * @param title 새로운 제목
     * @param content 새로운 내용
     * @param urls 새로 연결할 링크 목록
     * @param tags 새로 연결할 태그 목록
     * @return 수정된 큐레이션 객체
     */
    @Transactional
    fun updateCuration(
        curationId: Long, title: String?, content: String?, urls: List<String?>,
        tags: List<String?>, member: Member
    ): Curation {
        val curation = curationRepository.findById(curationId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 큐레이션을 찾을 수 없습니다."
                )
            }!!

        if (curation.member!!.id != member.id) {
            throw ServiceException("403", "권한이 없습니다.")
        }

        if (title != null) {
            curation.title = title
        }
        if (content != null) {
            curation.content = content
        }

        // 큐레이션 - 링크 연결 업데이트 (urls가 null이 아니고, 비어있지 않을 경우만)
        if (!urls.isNullOrEmpty()) {
            val curationLinks = urls.filterNotNull().map { url ->
                CurationLink().setCurationAndLink(curation, linkService.getLink(url))
            }
            curationLinkRepository.saveAll(curationLinks)
            curation.curationLinks.apply {
                clear()
                addAll(curationLinks)
            }
        }

        // 큐레이션 - 태그 연결 업데이트 (tags가 null이 아니고, 비어있지 않을 경우만)
        if (!tags.isNullOrEmpty()) {
            val curationTags = tags.filterNotNull().map { tag ->
                CurationTag().setCurationAndTag(curation, tagService.getTag(tag))
            }
            curationTagRepository.saveAll(curationTags)
            curation.tags.apply {
                clear()
                addAll(curationTags)
            }
        }

        val result = curationRepository.save(curation)

        // 큐레이션 수정 이벤트
        eventPublisher.publishEvent(
            CurationUpdateEvent(
                curationId = curation.id!!,
                imageUrls = curation.imageNames
            )
        )


        return result
    }

    /**
     * 큐레이션을 삭제합니다.
     * @param curationId 삭제할 큐레이션 ID
     */
    @Transactional
    fun deleteCuration(curationId: Long, member: Member) {
        // 큐레이션이 존재하는지 확인
        val curation = curationRepository.findById(curationId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 큐레이션을 찾을 수 없습니다."
                )
            }!!

        // 삭제 권한이 있는지 확인 (작성자와 요청자가 같은지 확인)
        println("어드민이야?" + member.isAdmin)
        println("어드민이야?" + curation.member!!.id)
        if (curation.member!!.id != member.id && !member.isAdmin) {
            throw ServiceException("403-1", "권한이 없습니다.") // 권한 없음
        }
        reportRepository.deleteByCurationId(curationId)
        curationLinkRepository.deleteByCurationId(curationId)
        curationTagRepository.deleteByCurationId(curationId)
        curationRepository.deleteById(curationId)

        // 조회 IP 정보 삭제
        val keys = redisTemplate.keys(VIEW_COUNT_KEY + curationId + "*")
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys)
        }

        // 일일 조회수 삭제
        redisTemplate.opsForZSet().remove(DAY_VIEW_COUNT_KEY, curationId.toString())

        // 큐레이션 삭제 이벤트
        eventPublisher.publishEvent(CurationDeleteEvent(curationId))
    }

    /**
     * 특정 큐레이션을 조회합니다.
     * @param curationId 조회할 큐레이션 ID
     * @return 조회된 큐레이션 객체
     */
    @Transactional
    fun getCuration(curationId: Long, request: HttpServletRequest): CurationDetailResDto {
        var ip = request.getHeader("X-Forwarded-For")

        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("X-Real-IP")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("X-RealIP")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("REMOTE_ADDR")
        }
        if (ip == null || ip.length == 0 || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }

        if (ip == "0:0:0:0:0:0:0:1" || ip == "127.0.0.1") {
            var address: InetAddress? = null
            try {
                address = InetAddress.getLocalHost()
            } catch (e: UnknownHostException) {
                throw RuntimeException(e)
            }
            ip = address.hostName + "/" + address.hostAddress
        }
        val key = VIEW_COUNT_KEY + curationId + ":" + ip
        println("Redis Key: $key")
        val isNewView = redisTemplate.opsForValue().setIfAbsent(key, true.toString(), Duration.ofDays(1))
        println("Redis Key Set? $isNewView | Key: $key")

        // 조회수 증가
        if (isNewView) {
            redisTemplate.opsForZSet()
                .incrementScore(DAY_VIEW_COUNT_KEY, curationId.toString(), 1.0) // curationId에 대한 조회수 증가
            redisTemplate.expire(DAY_VIEW_COUNT_KEY, Duration.ofDays(1)) // 1일 동안 유효하게 설정 (TTL)
        }

        val curation = curationRepository.findById(curationId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 큐레이션을 찾을 수 없습니다."
                )
            }!!

        // Redis의 좋아요 값(실제 값) 으로 수정
        val redisKey = "curation_like:$curationId"
        val likeCount = redisTemplate.opsForSet().size(redisKey)
        curation.likeCount = likeCount!!

        var isLogin = false
        var isLiked = false
        var isFollowed = false
        if (rq.isLogin) {
            isLogin = true
            val actor = rq.actor
            isLiked = isLikedByMember(curationId, actor.id!!)
            isFollowed = memberService.isFollowed(curation.memberId, actor.id)
        }

        if (isNewView) {
            curationViewService.increaseViewCount(curation)
        } else {
            println("조회수 증가 안 함 (이미 조회된 IP)")
        }

        return CurationDetailResDto.from(curation, isLiked, isFollowed, isLogin)
    }

    /**
     * 큐레이션을 검색합니다.
     * @param tags 태그 목록 (선택적)
     * @param title 제목 검색어 (선택적)
     * @param content 내용 검색어 (선택적)
     * @param order 정렬 기준
     * @return 검색된 큐레이션 목록
     */
    fun searchCurations(
        tags: List<String>?,
        title: String,
        content: String,
        author: String,
        order: SearchOrder = SearchOrder.LATEST,
        page: Int = 0,
        size: Int = 10
    ): CurationSearchResDto {
        var sort = Sort.by(Sort.Direction.DESC, "createdAt")
        if (order == SearchOrder.OLDEST) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt")
        }
        if (order == SearchOrder.LIKECOUNT) {
            sort = Sort.by(Sort.Direction.DESC, "likeCount")
        }
        val pageable: Pageable = PageRequest.of(page, size, sort)
        val curationPage: Page<Curation>
        val curations: List<Curation>

        if (tags == null || tags.isEmpty()) {
            // tags가 null이거나 빈 리스트인 경우 → 태그 필터 없이 검색
            curationPage = curationRepository.searchByFiltersWithoutTags(tags, title, content, author, pageable)
        } else {
            // tags가 null이 아니고 비어있지도 않은 경우 → 태그 필터 적용
            curationPage = curationRepository.searchByFilters(tags, tags.size, title, content, author, pageable)
        }

        curations = curationPage.content.map { curation ->
            val redisKey = "curation_like:${curation.id}"
            curation.likeCount = redisTemplate.opsForSet().size(redisKey)
            curation
        }

        return CurationSearchResDto.from(
            curations,
            curationPage.totalPages,
            curationPage.totalElements,
            curationPage.numberOfElements,
            curationPage.size
        )
    }


    @Transactional
    fun likeCuration(curationId: Long, memberId: Long) {
        // 큐레이션과 멤버를 찾음
        val curation = curationRepository.findById(curationId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 큐레이션을 찾을 수 없습니다."
                )
            }!!

        val member = memberRepository.findById(memberId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 멤버를 찾을 수 없습니다."
                )
            }

        // Redis Key 설정
        val redisKey = "curation_like:$curationId"
        val value = memberId.toString()

        // LUA 스크립트: 좋아요가 있으면 삭제, 없으면 추가
        val luaScript =
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
                    "   redis.call('SREM', KEYS[1], ARGV[1]); " +
                    "   return 0; " +  // 0이면 좋아요 삭제됨
                    "else " +
                    "   redis.call('SADD', KEYS[1], ARGV[1]); " +
                    "   return 1; " +  // 1이면 좋아요 추가됨
                    "end"

        // LUA 스크립트 실행
        val result = redisTemplate.execute(
            DefaultRedisScript(luaScript, Long::class.java),
            listOf(redisKey),
            value
        )
    }

    @Scheduled(fixedRate = 600000) // 10분마다 실행
    fun syncLikesToDatabase() {
        // Redis에서 모든 큐레이션의 좋아요 개수를 가져와서 DB에 업데이트
        val keys = redisTemplate.keys("curation_like:*")

        for (key in keys) {
            val parts = key.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val curationId = parts[1].toLong()

            // Like Repo에 좋아요 정보 추가
            val memberIds = redisTemplate.opsForSet().members(key)
            for (memberId in memberIds!!) {
                val curation = curationRepository.findById(curationId).get()
                val member = memberRepository.findByMemberId(memberId)
                likeRepository.save(Like.of(curation, member!!))
            }

            // Redis에서 좋아요 개수 구하기
            val redisKey = "curation_like:$curationId"
            val likesCount = redisTemplate.opsForSet().size(redisKey)

            if (likesCount != null) {
                // 큐레이션을 DB에 반영
                val curationOpt = curationRepository.findById(curationId)
                if (curationOpt.isPresent) {
                    val curation = curationOpt.get()
                    curation.likeCount = likesCount
                    curationRepository.save(curation)
                }
            }
        }
    }

    /**
     * 특정 큐레이션에 대한 좋아요 여부를 확인합니다.
     * @param curationId 큐레이션 ID
     * @param memberId 사용자 ID
     * @return 좋아요 여부 (true: 좋아요 누름, false: 좋아요 안 누름)
     */
    fun isLikedByMember(curationId: Long, memberId: Long): Boolean {
        val redisKey = "curation_like:$curationId"
        return redisTemplate.opsForSet().isMember(redisKey, memberId.toString())
    }

    /**
     * ✅ 특정 멤버가 팔로우하는 큐레이션 목록을 조회하는 메서드 추가
     * @param member 팔로우한 멤버
     * @return 팔로우한 멤버의 큐레이션 목록
     */
    fun getFollowingCurations(member: Member, page: Int, size: Int): List<CurationResDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val followingCurations = curationRepository.findFollowingCurations(member.id!!, pageable)
        return followingCurations.stream()
            .map { curation: Curation -> CurationResDto.from(curation) }
            .collect(Collectors.toList())
    }

    @Transactional
    fun reportCuration(curationId: Long, reportType: ReportType) {
        val actor = rq.actor
        val curation = curationRepository.findById(curationId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "존재하지 않는 큐레이션입니다."
                )
            }!!

        // 같은 사유로 이미 신고한 큐레이션 거부
        if (reportRepository.existsByCurationIdAndReporterIdAndReportType(curationId, actor.id, reportType)) {
            throw ServiceException("400-1", "이미 같은 사유로 신고한 큐레이션입니다.")
        }

        val report = Report(
            curation = curation,
            reportType = reportType,
            reporter = actor,
        )

        reportRepository.save(report)
    }

    fun findAllByMember(member: Member): List<Curation?>? {
        return curationRepository.findAllByMember(member)
    }

    @Transactional(readOnly = true)
    fun getTrendingCuration(): TrendingCurationResDto {
        val topCurations = redisTemplate?.opsForZSet()
            ?.reverseRange(DAY_VIEW_COUNT_KEY, 0, 2)
            ?.mapNotNull { curationId ->
                curationRepository.findById(curationId.toLong())?.orElseGet {
                    redisTemplate.opsForZSet().remove(DAY_VIEW_COUNT_KEY, curationId)
                    null
                }
            }
            ?.onEach { curation ->
                curation.viewCount = redisTemplate.opsForZSet()
                    .score(DAY_VIEW_COUNT_KEY, curation.id.toString())?.toLong() ?: 0L
            }
            ?.sortedByDescending { it.viewCount }

        return when {
            topCurations.isNullOrEmpty() -> {
                TrendingCurationResDto.from(
                    curationRepository.findTop3ByOrderByViewCountDesc().sortedByDescending { it.viewCount },
                )
            }
            else -> TrendingCurationResDto.from(topCurations)
        }
    }




    @Transactional(readOnly = true)
    fun searchCurationByUserName(username: String?, page: Int, size: Int): List<CurationResDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val author = memberRepository.findByUsername(username)
            ?: throw ServiceException(
                "404-1",
                "작성자가 존재하지 않습니다."
            )

        return curationRepository.findAllByMember(author, pageable).stream()
            .map<CurationResDto> { curation: Curation -> CurationResDto.from(curation) }
            .collect(Collectors.toUnmodifiableList<CurationResDto>())
    }

    companion object {
        private const val VIEW_COUNT_KEY = "view_count:" // Redis 키 접두사
        private const val DAY_VIEW_COUNT_KEY = "day_view_count:" // Redis 키 접두사
        private const val LIKE_COUNT_KEY = "curation:like_count" // 좋아요 수 저장
    }
}