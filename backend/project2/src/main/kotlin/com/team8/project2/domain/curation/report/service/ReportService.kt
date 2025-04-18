package com.team8.project2.domain.curation.report.service

import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.report.dto.ReportDto
import com.team8.project2.domain.curation.report.dto.ReportedCurationsDetailResDto
import com.team8.project2.domain.curation.report.dto.ReportedCurationsDetailResDto.Companion.from
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.member.entity.Member
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@RequiredArgsConstructor
class ReportService(
    private val reportRepository: ReportRepository,
    private val curationRepository: CurationRepository,
) {
    @Transactional
    fun getReportedCurationsDetailResDtos(reportedCurationIds: List<Long>): List<ReportedCurationsDetailResDto> {
        // 신고된 큐레이션 ID를 기반으로 큐레이션 목록 조회
        val curations = curationRepository.findByIdIn(reportedCurationIds)
        // 신고된 큐레이션 ID를 기반으로 신고(Report) 목록 조회
        val reports = reportRepository.findByCurationIdIn(reportedCurationIds)
        // 큐레이션 ID별 신고 목록을 매핑
        val reportsByCuration = reports.groupBy { it.curation.id }

        return curations
            .map { curation -> from(curation, reportsByCuration[curation.id] ?: emptyList()) }
    }

    @Transactional
    fun findAllByReporter(member: Member): List<ReportDto> {
        // 사용자에 의해 신고한 모든 리포트 조회
        val reports = reportRepository.findAllByReporter(member)

        // Report 엔티티를 ReportDto로 변환하여 반환
        return reports
            .map { obj: Report -> ReportDto.from(obj) } // ReportDto의 from() 메서드를 사용하여 변환
    }
}
