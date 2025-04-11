package com.team8.project2.domain.curation.report.dto

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType

data class ReportedCurationsDetailResDto(
    val curationId: Long,
    val curationTitle: String,
    val authorName: String,
    val reportTypeCounts: List<ReportCountResDto>
) {
    data class ReportCountResDto(
        val reportType: ReportType,
        val count: Long
    )

    companion object {
        fun from(curation: Curation, reports: List<Report>): ReportedCurationsDetailResDto {
            val reportTypeCounts = reports
                .groupingBy { it.reportType }
                .eachCount()
                .map { (type, count) ->
                    ReportCountResDto(type, count.toLong())
                }

            return ReportedCurationsDetailResDto(
                curationId = curation.id ?: error("curation.id must not be null"),
                curationTitle = curation.title,
                authorName = curation.memberName,
                reportTypeCounts = reportTypeCounts
            )
        }
    }
}
