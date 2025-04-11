package com.team8.project2.domain.curation.report.entity

import com.team8.project2.global.exception.ServiceException

enum class ReportType(val description: String) {
    ABUSE("욕설 및 비방"),
    SPAM("스팸 및 광고"),
    FALSE_INFO("허위 정보 또는 불법 콘텐츠"),
    INAPPROPRIATE("부적절한 내용 (음란물, 폭력 등)");

    companion object {
        @JvmStatic
        fun fromString(value: String?): ReportType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw ServiceException("400-1", "잘못된 신고 유형입니다.")
    }
}

