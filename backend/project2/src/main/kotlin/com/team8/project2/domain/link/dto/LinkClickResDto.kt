package com.team8.project2.domain.link.dto

import com.team8.project2.domain.link.entity.Link
import java.time.LocalDateTime

data class LinkClickResDto(
    var id: Long?,
    var url: String?,
    var title: String?,
    var description: String?,
    var metaImageUrl: String?,
    var click: Int,
    var createdAt: LocalDateTime?
) {
    companion object {
        fun fromEntity(link: Link): LinkClickResDto {
            return LinkClickResDto(
                id = link.id,
                url = link.url,
                title = link.title,
                description = link.description,
                metaImageUrl = link.metaImageUrl,
                click = link.click,
                createdAt = link.createdAt
            )
        }
    }
}

