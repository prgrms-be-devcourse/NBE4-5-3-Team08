package com.team8.project2.domain.curation.curation.repository

import com.team8.project2.domain.curation.curation.dto.CurationProjectionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CurationRepositoryCustom {
    fun searchByProjection(
        tags: List<String>?,
        tagsSize: Int,
        title: String?,
        content: String?,
        author: String?,
        pageable: Pageable
    ): Page<CurationProjectionDto>
}
