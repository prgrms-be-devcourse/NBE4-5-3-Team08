package com.team8.project2.domain.comment.dto

import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 댓글(Comment) 데이터 전송 객체(DTO) 클래스입니다.
 * 엔티티와의 변환을 지원하며, 클라이언트와의 데이터 교환을 담당합니다.
 */
data class CommentDto(

	/** 댓글 ID */
	val id: Long? = null,

	/** 댓글 작성자의 사용자 ID */
	val authorName: String?,

	/** 댓글 작성자의 프로필 이미지 */
	val authorProfileImageUrl: String?,

	/** 댓글 내용 */
	val content: String,

	/** 댓글 생성 시간 */
	val createdAt: LocalDateTime? = null,

	/** 댓글 수정 시간 */
	val modifiedAt: LocalDateTime? = null

) {

	/**
	 * DTO(CommentDto)를 엔티티(Comment)로 변환합니다.
	 * @param member 댓글 작성자
	 * @param curation 댓글이 속한 큐레이션 엔티티
	 * @return 변환된 댓글 엔티티
	 */
	fun toEntity(member: Member, curation: Curation): Comment {
		return Comment(
			author = member,
			curation = curation,
			content = content
		)
	}

	companion object {
		/**
		 * 엔티티(Comment) 객체를 DTO(CommentDto)로 변환합니다.
		 * @param comment 변환할 댓글 엔티티
		 * @return 변환된 댓글 DTO
		 */
		fun fromEntity(comment: Comment): CommentDto {
			return CommentDto(
				id = comment.id,
				authorName = comment.author.getUsername(),
				authorProfileImageUrl = comment.author.profileImage,
				content = comment.content,
				createdAt = comment.createdAt,
				modifiedAt = comment.modifiedAt
			)
		}
	}
}
