package com.team8.project2.domain.curation.curation.controller

import CurationDetailResDto
import com.team8.project2.domain.curation.curation.entity.Curation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import com.team8.project2.domain.curation.curation.dto.*
import com.team8.project2.domain.curation.curation.entity.SearchOrder
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.curation.tag.dto.TagResDto
import com.team8.project2.domain.curation.tag.service.TagService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.playlist.dto.PlaylistDto
import com.team8.project2.domain.playlist.service.PlaylistService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

/**
 * 큐레이션(Curation) API 컨트롤러 클래스입니다.
 * 큐레이션 생성, 수정, 삭제, 조회 및 검색 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/curation")
class ApiV1CurationController(
    private val curationService: CurationService,
    private val rq: Rq,
    private val playlistService: PlaylistService,
    private val tagService: TagService
) {
    /**
     * 새로운 큐레이션을 생성합니다.
     *
     * @param curationReq 큐레이션 생성 요청 데이터
     * @return 생성된 큐레이션 정보 응답
     */
    @PostMapping
    fun createCuration(@RequestBody curationReq: CurationReqDTO): RsData<CurationResDto> {
        val member: Member = rq.actor

        val createdCuration: Curation = curationService.createCuration(
            curationReq.title,
            curationReq.content,
            curationReq.linkReqDtos!!.map { it.url },
            curationReq.tagReqDtos!!.map { it.name },
            member
        )

        return RsData("201-1", "글이 성공적으로 생성되었습니다.", CurationResDto.from(createdCuration))
    }

    /**
     * 기존 큐레이션을 수정합니다.
     *
     * @param id 큐레이션 ID
     * @param curationReq 큐레이션 수정 요청 데이터
     * @return 수정된 큐레이션 정보 응답
     */
    @PutMapping("/{id}")
    fun updateCuration(
        @PathVariable id: Long,
        @RequestBody curationReq: CurationReqDTO
    ): RsData<CurationResDto> {
        val member: Member = rq.actor

        val updatedCuration: Curation = curationService.updateCuration(
            id,
            curationReq.title,
            curationReq.content,
            curationReq.linkReqDtos!!.map { it.url },
            curationReq.tagReqDtos!!.map { it.name },
            member
        )

        return RsData("200-1", "글이 성공적으로 수정되었습니다.", CurationResDto.from(updatedCuration))
    }

    /**
     * 특정 큐레이션을 삭제합니다.
     *
     * @param id 큐레이션 ID
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/{id}")
    fun deleteCuration(@PathVariable id: Long): RsData<Void?> {
        val member: Member = rq.actor
        curationService.deleteCuration(id, member)
        return RsData("204-1", "글이 성공적으로 삭제되었습니다.", null)
    }

    /**
     * 특정 큐레이션을 조회합니다.
     *
     * @param id 큐레이션 ID
     * @return 조회된 큐레이션 정보 응답
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getCuration(@PathVariable id: Long, request: HttpServletRequest): RsData<CurationDetailResDto> {
        // 큐레이션 서비스 호출 시 IP를 전달
        val curationDetailResDto = curationService.getCuration(id, request)
        return RsData("200-1", "조회 성공", curationDetailResDto)
    }

    /**
     * 큐레이션을 검색하거나 전체 조회합니다.
     *
     * @param tags    태그 목록 (선택적)
     * @param title   제목 검색어 (선택적)
     * @param content 내용 검색어 (선택적)
     * @param order   정렬 기준 (기본값: 최신순)
     * @return 검색된 큐레이션 목록 응답
     */
    @GetMapping
    @Transactional(readOnly = true)
    fun searchCuration(
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(defaultValue = "", required = false) title: String,
        @RequestParam(defaultValue = "", required = false) content: String,
        @RequestParam(defaultValue = "", required = false) author: String,
        @RequestParam(defaultValue = "LATEST", required = false) order: SearchOrder,
        @RequestParam(defaultValue = "0", required = false) page: Int,
        @RequestParam(defaultValue = "20", required = false) size: Int
    ): RsData<CurationSearchResDto> {
        val curationSearchResDto = curationService.searchCurations(tags, title, content, author, order, page, size)
        return RsData("200-1", "글이 검색되었습니다.", curationSearchResDto)
    }

    @GetMapping("/author/{username}")
    fun searchCurationByUserName(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RsData<List<CurationResDto>> {
        val result = curationService.searchCurationByUserName(username, page, size)
        return RsData("200-1", "작성자로 큐레이션이 검색되었습니다.", result)
    }

    /**
     * 특정 큐레이션에 좋아요를 추가합니다.
     *
     * @param id 큐레이션 ID
     * @return 좋아요 성공 응답
     */
    @PostMapping("/like/{id}")
    @PreAuthorize("isAuthenticated()")
    fun likeCuration(@PathVariable id: Long): RsData<Void?> {
        val memberId = rq.actor.id
        curationService.likeCuration(id, memberId!!)
        return RsData("200-1", "글에 좋아요를 했습니다.", null)
    }

    /**
     * 특정 큐레이션에 대해 사용자가 좋아요를 눌렀는지 확인합니다.
     *
     * @param id 큐레이션 ID
     * @return 좋아요 여부 응답 (true: 좋아요 누름, false: 좋아요 안 누름)
     */
    @GetMapping("/like/{id}/status")
    fun isCurationLiked(@PathVariable id: Long): RsData<Boolean> {
        val memberId = rq.actor.id
        val isLiked = curationService.isLikedByMember(id, memberId!!)
        return RsData("200-1", "좋아요 여부 확인 성공", isLiked)
    }

    /**
     * 사용자가 팔로우 중인 큐레이터의 큐레이션을 조회합니다.
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 큐레이션 목록 응답
     */
    @GetMapping("/following")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    fun followingCuration(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RsData<List<CurationResDto>> {
        val actor = rq.actor
        val curations = curationService.getFollowingCurations(actor, page, size)
        return RsData("200-1", "팔로우중인 큐레이터의 큐레이션이 조회되었습니다.", curations)
    }

    /**
     * 큐레이션 신고 요청 DTO
     *
     * @param reportType 신고 유형
     */
    data class CurationReportReqDto(
        @field:NotNull val reportType: String
    )

    /**
     * 특정 큐레이션을 신고합니다.
     *
     * @param id 큐레이션 ID
     * @param curationReportReqDto 신고 요청 데이터
     * @return 신고 접수 응답
     */
    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    fun reportCuration(
        @PathVariable id: Long,
        @RequestBody @Valid curationReportReqDto: CurationReportReqDto
    ): RsData<Unit> {
        val reportType = ReportType.fromString(curationReportReqDto.reportType)
        curationService.reportCuration(id, reportType)
        return RsData("200-1", "신고가 접수되었습니다.")
    }

    /**
     * 특정 큐레이션에 속한 플레이리스트 목록을 조회합니다.
     *
     * @param curationId
     * @return
     */
    @GetMapping("/{curationId}/playlists")
    @Transactional(readOnly = true)
    fun getPlaylistsForCuration(@PathVariable curationId: Long): RsData<List<PlaylistDto>> {
        val member = rq.actor
        val playlists = playlistService.getPlaylistsByMemberAndCuration(member, curationId)
        return RsData.success("플레이리스트 조회 성공", playlists)
    }

    @GetMapping("/trending-tag")
    fun trendingTag(): RsData<TagResDto> {
        val tagResDto = tagService.getTrendingTag()
        return RsData("200-1", "트렌딩 태그가 조회되었습니다.", tagResDto)
    }

    @GetMapping("/trending-curation")
    fun trendingCuration(): RsData<TrendingCurationResDto> {
        val trendingCurationResDto = curationService.getTrendingCuration()
        return RsData("200-1", "트렌딩 큐레이션이 조회되었습니다.", trendingCurationResDto)
    }



}
