package com.team8.project2.domain.curation.report.repository

import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : JpaRepository<Report, Long> {
    fun existsByCurationIdAndReporterIdAndReportType(
        curationId: Long,
        id: Long?,
        reportType: ReportType,
    ): Boolean

    fun findByCurationIdIn(reportedCurationIds: List<Long>): List<Report>

    fun deleteByCurationId(curationId: Long)

    fun findAllByReporter(reporter: Member): List<Report>
}
