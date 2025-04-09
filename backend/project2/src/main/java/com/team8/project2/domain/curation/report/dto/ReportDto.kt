package com.team8.project2.domain.curation.report.dto

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType
import java.time.LocalDateTime

data class ReportDto(
    val reportId: Long, // 신고 ID
    val curationId: Long?, // 큐레이션 ID
    val curationTitle: String, // 큐레이션 제목
    val reportType: ReportType, // 신고 유형
    val reportDate: LocalDateTime, // 신고 날짜
) {
    companion object {
        @JvmStatic
        fun from(report: Report): ReportDto {
            val curation: Curation = report.curation
            return ReportDto(
                reportId = report.id ?: throw IllegalArgumentException("report.id must not be null"),
                curationId = curation.id,
                curationTitle = curation.title,
                reportType = report.reportType,
                reportDate = report.reportDate ?: throw IllegalArgumentException("reportDate must not be null"),
            )
        }
    }
}
