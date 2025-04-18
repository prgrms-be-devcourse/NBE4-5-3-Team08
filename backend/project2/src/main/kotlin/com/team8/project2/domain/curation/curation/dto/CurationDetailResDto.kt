package com.team8.project2.domain.curation.curation.dto

import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.comment.entity.ReplyComment
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.link.entity.Link
import java.time.LocalDateTime

data class CurationDetailResDto(
    val id: Long?,
    val title: String,
    val content: String,
    val authorId: Long?,
    val authorName: String,
    val authorImgUrl: String?,
    val urls: List<LinkResDto>,
    val tags: List<TagResDto>,
    val comments: List<CommentResDto>,
    val createdAt: LocalDateTime?,
    val modifiedAt: LocalDateTime?,
    val likeCount: Long,
    val viewCount: Long,
    val isLiked: Boolean,
    val isFollowed: Boolean,
    val isLogin: Boolean
) {
    data class LinkResDto(
        val id: Long?,
        val url: String?,
        val title: String?,
        val description: String?,
        val imageUrl: String?,
        val click: Int
    ) {
        companion object {
            fun from(link: Link): LinkResDto {
                return LinkResDto(
                    id = link.id,
                    url = link.url,
                    title = link.title,
                    description = link.description,
                    imageUrl = link.metaImageUrl,
                    click = link.click
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

    data class CommentResDto(
        val commentId: Long?,
        val authorId: Long?,
        val authorName: String,
        val authorImgUrl: String,
        val content: String,
        val createdAt: LocalDateTime?,
        val modifiedAt: LocalDateTime?,
        val replies: List<ReplyCommentResDto>
    ) {
        companion object {
            fun from(comment: Comment): CommentResDto {
                return CommentResDto(
                    commentId = comment.id,
                    authorId = comment.author.id,
                    authorName = comment.author.getUsername(),
                    authorImgUrl = comment.author.profileImage!!,
                    content = comment.content,
                    createdAt = comment.createdAt,
                    modifiedAt = comment.modifiedAt,
                    replies = comment.replyComments.map { ReplyCommentResDto.from(it) }
                )
            }
        }
    }

    companion object {
        fun from(
            curation: Curation,
            isLiked: Boolean,
            isFollowed: Boolean,
            isLogin: Boolean
        ): CurationDetailResDto {
            return CurationDetailResDto(
                id = curation.id,
                title = curation.title,
                content = curation.content,
                authorId = curation.member.id,
                authorName = curation.member.getUsername(),
                authorImgUrl = curation.member.profileImage,
                urls = curation.curationLinks.map { LinkResDto.from(it.link!!) },
                tags = curation.tags.map { TagResDto.from(it.tag) },
                comments = curation.comments.map { CommentResDto.from(it) },
                createdAt = curation.createdAt,
                modifiedAt = curation.modifiedAt,
                likeCount = curation.likeCount,
                viewCount = curation.viewCount,
                isLiked = isLiked,
                isFollowed = isFollowed,
                isLogin = isLogin
            )
        }
    }

    data class ReplyCommentResDto(
        val commentId: Long?,
        val authorId: Long?,
        val authorName: String,
        val authorImgUrl: String,
        val content: String,
        val createdAt: LocalDateTime?,
        val modifiedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(reply: ReplyComment): ReplyCommentResDto {
                return ReplyCommentResDto(
                    commentId = reply.id,
                    authorId = reply.author.id,
                    authorName = reply.author.getUsername(),
                    authorImgUrl = reply.author.profileImage!!,
                    content = reply.content,
                    createdAt = reply.createdAt,
                    modifiedAt = reply.modifiedAt,
                )
            }
        }
    }
}
