package com.team8.project2.domain.link.controller

import com.team8.project2.domain.link.dto.LinkClickResDto
import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.global.dto.RsData
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 링크(Link) 관련 API 컨트롤러 클래스입니다.
 * 링크 추가, 수정, 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/link")
class ApiV1LinkController(
    private val linkService: LinkService
) {

    /**
     * 새로운 링크를 추가합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addLink(@RequestBody @Valid linkDTO: LinkReqDTO): RsData<Link> {
        val link = linkService.addLink(linkDTO)
        return RsData.success("링크가 성공적으로 추가되었습니다.", link)
    }

    /**
     * 기존 링크를 수정합니다.
     */
    @PutMapping("/{linkId}")
    fun updateLink(
        @PathVariable linkId: Long,
        @RequestBody @Valid linkDTO: LinkReqDTO
    ): RsData<Link> {
        val updatedLink = linkService.updateLink(linkId, linkDTO.url)
        return RsData.success("링크가 성공적으로 수정되었습니다.", updatedLink)
    }

    /**
     * 특정 링크를 삭제합니다.
     */
    @DeleteMapping("/{linkId}")
    fun deleteLink(@PathVariable linkId: Long): RsData<Void> {
        linkService.deleteLink(linkId)
        return RsData("204-1", "링크가 성공적으로 삭제되었습니다.")
    }

    /**
     * 특정 링크를 조회하고 조회수를 증가시킵니다.
     */
    @GetMapping("/{linkId}")
    fun getLink(
        @PathVariable linkId: Long,
        request: HttpServletRequest
    ): RsData<LinkClickResDto> {
        val linkClickResDto = linkService.getLinkAndIncrementClick(linkId, request)
        return RsData.success("링크가 성공적으로 조회되었습니다.", linkClickResDto)
    }
}
