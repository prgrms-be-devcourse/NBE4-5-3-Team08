package com.team8.project2.domain.curation.curation.dto

import com.team8.project2.domain.curation.curation.entity.Curation

data class TrendingCurationResDto(
    val curations: List<CurationSummaryResDto>
) {
    companion object {
        fun from(curations: List<Curation>): TrendingCurationResDto {
            return TrendingCurationResDto(
                curations = curations.map { CurationSummaryResDto.from(it) }
            )
        }
    }
}
