package com.team8.project2.domain.comment.dto

import com.team8.project2.domain.comment.entity.ReplyComment
import java.time.LocalDateTime

/**
 * 답글(ReplyComment) 데이터 전송 객체(DTO) 클래스입니다.
 * 엔티티와의 변환을 지원하며, 클라이언트와의 데이터 교환을 담당합니다.
 */
data class ReplyCommentDto(

    /** 답글 ID */
    val id: Long?,

    /** 답글 작성자의 id */
    val authorId: Long,

    /** 답글 작성자의 사용자명 */
    val authorName: String,

    /** 답글 작성자의 프로필 이미지 */
    val authorProfileImageUrl: String,

    /** 답글 내용 */
    val content: String,

    /** 답글 생성 시간 */
    val createdAt: LocalDateTime?,

    /** 답글 수정 시간 */
    val modifiedAt: LocalDateTime?

) {
    companion object {
        /**
         * 엔티티(ReplyComment) 객체를 DTO(ReplyCommentDto)로 변환합니다.
         * @param reply 변환할 답글 엔티티
         * @return 변환된 답글 DTO
         */
        fun fromEntity(reply: ReplyComment): ReplyCommentDto {
            return ReplyCommentDto(
                id = reply.id,
                authorId = reply.author.id!!,
                authorName = reply.author.getUsername(),
                authorProfileImageUrl = reply.author.profileImage.toString(),
                content = reply.content,
                createdAt = reply.createdAt,
                modifiedAt = reply.modifiedAt
            )
        }
    }
}
