package com.team8.project2.domain.report.service

import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.curation.report.service.ReportService
import com.team8.project2.domain.member.repository.MemberRepository
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
internal class ReportServiceTest {
    @Autowired
    lateinit var reportService: ReportService

    @Autowired
    lateinit var reportRepository: ReportRepository

    @Autowired
    lateinit var curationRepository: CurationRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Test
    @DisplayName("신고가 여러 건 존재할 때 유형별 개수를 올바르게 집계한다")
    fun testGetReportedCurationsDetail() {
        // given
        val curation = curationRepository.findById(1L).orElseThrow()
        val reporter1 = memberRepository.findById(2L).orElseThrow()
        val reporter2 = memberRepository.findById(3L).orElseThrow()

        reportRepository.save(Report(
            curation = curation,
            reportType = ReportType.ABUSE,
            reporter = reporter1,
        ))
        reportRepository.save(Report(
            curation = curation,
            reportType = ReportType.SPAM,
            reporter = reporter2,
        ))
        reportRepository.save(Report(
            curation = curation,
            reportType = ReportType.ABUSE,
            reporter = reporter1,
        ))

        // when
        val result = reportService.getReportedCurationsDetailResDtos(listOf(curation.id!!))

        // then
        assertThat(result).hasSize(1)
        val dto = result[0]
        assertThat(dto.reportTypeCounts).hasSize(2) // ABUSE, SPAM
        assertThat(dto.reportTypeCounts).anySatisfy { type ->
            assertThat(type.reportType).isIn(ReportType.ABUSE, ReportType.SPAM)
        }
    }
}
