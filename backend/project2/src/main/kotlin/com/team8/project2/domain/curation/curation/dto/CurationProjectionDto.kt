package com.team8.project2.domain.curation.curation.dto

import com.querydsl.core.annotations.QueryProjection
import java.time.LocalDateTime

data class CurationProjectionDto @QueryProjection constructor(
    val id: Long,
    val title: String,
    val content: String,
    val viewCount: Long,
    val authorName: String,
    val memberImgUrl: String?,
    val createdAt: LocalDateTime?,
    val modifiedAt: LocalDateTime?,
    val commentCount: Int,
    val tags: List<String>,
    val urls: List<CurationResDto.LinkResDto>
)
