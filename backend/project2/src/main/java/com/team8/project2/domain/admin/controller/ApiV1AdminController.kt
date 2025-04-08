package com.team8.project2.domain.admin.controller

import com.team8.project2.domain.admin.dto.StatsResDto
import com.team8.project2.domain.admin.service.AdminService
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.report.dto.ReportedCurationsDetailResDto
import com.team8.project2.domain.curation.report.service.ReportService
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import com.team8.project2.global.exception.ServiceException
import org.springframework.web.bind.annotation.*
import java.util.function.Consumer

@RestController
@RequestMapping("/api/v1/admin")
class ApiV1AdminController(
    private val curationService: CurationService,
    private val adminService: AdminService,
    private val rq: Rq,
    private val memberService: MemberService,
    private val commentService: CommentService,
    private val reportService: ReportService

) {

    // ✅ 큐레이션 삭제
    @DeleteMapping("/curations/{curationId}")
    fun deleteCuration(@PathVariable curationId: Long): RsData<String?> {
        val member = rq.actor

        curationService.deleteCuration(curationId, member)
        return RsData("204-1", "글이 성공적으로 삭제되었습니다.", null)
    }

    // ✅ 멤버 삭제
    @DeleteMapping("/members/{memberId}")
    fun deleteMember(@PathVariable memberId: Long): RsData<String> {
        val member = memberService.findById(memberId)
            .orElseThrow {
                ServiceException(
                    "404-1",
                    "해당 회원을 찾을 수 없습니다."
                )
            }
        adminService.deleteMember(member)
        return RsData.success("멤버가 삭제되었습니다.")
    }

    // ✅ 일정 개수 이상 신고된 큐레이션 조회
    @GetMapping("/reported-curations")
    fun getReportedCurations(
        @RequestParam(defaultValue = "5") minReports: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RsData<List<Long>> {
        return RsData.success("신고된 큐레이션 목록 조회 성공", adminService.getReportedCurations(minReports, page, size))
    }

    // ✅ 일정 개수 이상 신고된 큐레이션 상세 조회
    @GetMapping("/reported-curations-detail")
    fun getReportedCurationsDetail(
        @RequestParam(defaultValue = "5") minReports: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RsData<List<ReportedCurationsDetailResDto>> {
        val reportedcurations = adminService.getReportedCurations(minReports, page, size)
        reportedcurations.forEach(Consumer { println(it) })
        return RsData.success("신고된 큐레이션 목록 조회 성공", reportService.getReportedCurationsDetailResDtos(reportedcurations))
    }

    // ✅ 큐레이션 & 플레이리스트 통계 조회
    @GetMapping("/stats")
    fun getStats(): RsData<StatsResDto> {
        return RsData.success(
            "트래픽 통계 조회 성공",
            adminService.getCurationAndPlaylistStats()
        )
    }
}
