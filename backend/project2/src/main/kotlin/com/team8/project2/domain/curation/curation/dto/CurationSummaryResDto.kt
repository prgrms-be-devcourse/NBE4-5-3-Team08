package com.team8.project2.domain.curation.curation.dto

import com.team8.project2.domain.curation.curation.entity.Curation

data class CurationSummaryResDto(
    val curationId: Long?,
    val title: String,
    val authorName: String,
    val viewCount: Long
) {
    companion object {
        fun from(curation: Curation): CurationSummaryResDto {
            return CurationSummaryResDto(
                curationId = curation.id,
                title = curation.title,
                authorName = curation.memberName,
                viewCount = curation.viewCount
            )
        }
    }
}
