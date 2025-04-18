package com.team8.project2.domain.curation.tag.dto

import com.team8.project2.domain.curation.tag.entity.Tag

data class TagResDto(
    val tags: List<String>,
) {
    companion object {
        fun of(topTags: List<Tag>): TagResDto =
            TagResDto(
                tags = topTags.map { it.name },
            )
    }
}
