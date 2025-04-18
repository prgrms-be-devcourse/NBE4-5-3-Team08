package com.team8.project2.domain.curation.curation.dto

import com.team8.project2.domain.curation.curation.entity.Curation

data class CurationSearchResDto(
    val curations: List<CurationResDto>,
    val totalPages: Int,
    val totalElements: Long,
    val numberOfElements: Int,
    val size: Int
) {
    companion object {
        fun from(
            curations: List<Curation>,
            totalPages: Int,
            totalElements: Long,
            numberOfElements: Int,
            size: Int
        ): CurationSearchResDto {
            return CurationSearchResDto(
                curations = curations.map { CurationResDto.from(it) },
                totalPages = totalPages,
                totalElements = totalElements,
                numberOfElements = numberOfElements,
                size = size
            )
        }
    }
}
