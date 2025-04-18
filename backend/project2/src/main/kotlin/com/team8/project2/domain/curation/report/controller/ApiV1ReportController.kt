package com.team8.project2.domain.curation.report.controller

import com.team8.project2.domain.curation.report.dto.ReportDto
import com.team8.project2.domain.curation.report.service.ReportService
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import com.team8.project2.global.exception.ServiceException
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
class ApiV1ReportController(
    private var rq: Rq,
    private val memberService: MemberService,
    private val reportService: ReportService
) {
    // ✅ 신고 조회
    @GetMapping("/myreported/{loginId}")
    fun getReports(@PathVariable loginId: Long): RsData<List<ReportDto>> {
        val member = rq.actor

        require(member.id == loginId) {
            throw ServiceException("403-1", "회원 정보가 일치하지 않습니다.")
        }

        val reporter = memberService.findById(loginId)

        val reports = reportService.findAllByReporter(reporter)
        return RsData("200-1", "글이 성공적으로 조회되었습니다.", reports)
    }
}
