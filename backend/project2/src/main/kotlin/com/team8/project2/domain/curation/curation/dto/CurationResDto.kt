package com.team8.project2.domain.curation.curation.dto

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.link.entity.Link
import java.time.LocalDateTime

data class CurationResDto(
    val id: Long?,
    val title: String,
    val content: String,
    val viewCount: Long,
    val authorName: String,
    val memberImgUrl: String?,
    val urls: List<LinkResDto>,
    val tags: List<TagResDto>,
    val createdAt: LocalDateTime?,
    val modifiedAt: LocalDateTime?,
    val likeCount: Long,
    val commentCount: Int
) {
    data class LinkResDto(
        val id: Long?,
        val url: String?,
        val title: String?,
        val description: String?,
        val imageUrl: String?
    ) {
        companion object {
            fun from(link: Link?): LinkResDto {
                return LinkResDto(
                    id = link!!.id,
                    url = link.url,
                    title = link.title,
                    description = link.description,
                    imageUrl = link.metaImageUrl
                )
            }
        }
    }

    data class TagResDto(
        val name: String
    ) {
        companion object {
            fun from(tag: Tag): TagResDto {
                return TagResDto(
                    name = tag.name
                )
            }
        }
    }

    companion object {
        fun from(curation: Curation): CurationResDto {
            return CurationResDto(
                id = curation.id,
                title = curation.title,
                content = curation.content,
                urls = curation.curationLinks.map { LinkResDto.from(it.link) },
                tags = curation.tags.map { TagResDto.from(it.tag) },
                createdAt = curation.createdAt,
                modifiedAt = curation.modifiedAt,
                authorName = curation.memberName,
                memberImgUrl = curation.memberImgUrl,
                likeCount = curation.likeCount,
                viewCount = curation.viewCount,
                commentCount = curation.commentCount
            )
        }
    }
}
