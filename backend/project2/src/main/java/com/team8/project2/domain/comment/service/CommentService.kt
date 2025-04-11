package com.team8.project2.domain.comment.service

import com.team8.project2.domain.comment.dto.CommentDto
import com.team8.project2.domain.comment.dto.ReplyCommentDto
import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.comment.entity.ReplyComment
import com.team8.project2.domain.comment.repository.CommentRepository
import com.team8.project2.domain.comment.repository.ReplyCommentRepository
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.ServiceException
import com.team8.project2.global.security.SecurityUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 댓글 및 답글(Comment, ReplyComment)에 대한 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 댓글 작성, 수정, 삭제, 조회 및 권한 확인 기능을 제공합니다.
 */
@Service
@Transactional
class CommentService(
	private val commentRepository: CommentRepository,
	private val curationRepository: CurationRepository,
	private val replyCommentRepository: ReplyCommentRepository,
	private val rq: Rq
) {

	/**
	 * 새로운 댓글 생성
	 */
	fun createComment(author: Member, curationId: Long, commentDto: CommentDto): CommentDto {
		val curation = curationRepository.findById(curationId)
			.orElseThrow { ServiceException("404-1", "해당 큐레이션을 찾을 수 없습니다. (id: $curationId)") }

		val comment = commentDto.toEntity(author, curation)
		val savedComment = commentRepository.save(comment)
		return CommentDto.fromEntity(savedComment)
	}

	/**
	 * 특정 큐레이션에 속한 댓글 목록 조회
	 */
	@Transactional(readOnly = true)
	fun getCommentsByCurationId(curationId: Long): List<CommentDto> {
		return commentRepository.findByCurationId(curationId)
			.map { CommentDto.fromEntity(it) }
	}

	/**
	 * 댓글 삭제
	 */
	fun deleteComment(commentId: Long) {
		val comment = commentRepository.findById(commentId)
			.orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }
		commentRepository.delete(comment)
	}

	/**
	 * 댓글 내용 수정
	 */
	fun updateComment(commentId: Long, commentDto: CommentDto): CommentDto {
		val comment = commentRepository.findById(commentId)
			.orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }

		comment.updateContent(commentDto.content)
		return CommentDto.fromEntity(comment)
	}

	/**
	 * 댓글 수정 권한 확인
	 */
	fun canEditComment(commentId: Long, userDetails: UserDetails?): Boolean {
		if (userDetails == null) throw ServiceException("401-1", "사용자 정보가 없습니다.")

		val comment = commentRepository.findById(commentId)
			.orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }

		if (userDetails !is SecurityUser) {
			throw ServiceException("401-3", "잘못된 인증 정보입니다.")
		}

		if (comment.author.id != userDetails.id) {
			throw ServiceException("403-2", "댓글을 수정할 권한이 없습니다.")
		}

		return true
	}

	/**
	 * 댓글 삭제 권한 확인
	 */
	fun canDeleteComment(commentId: Long, userDetails: UserDetails?): Boolean {
		if (userDetails == null) throw ServiceException("401-1", "사용자 정보가 없습니다.")

		val comment = commentRepository.findById(commentId)
			.orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }

		if (userDetails !is SecurityUser) {
			throw ServiceException("401-3", "잘못된 인증 정보입니다.")
		}

		if (comment.author.id != userDetails.id) {
			throw ServiceException("403-2", "댓글을 삭제할 권한이 없습니다.")
		}

		return true
	}


	/**
	 * 특정 사용자(authorId)가 작성한 모든 댓글 조회
	 */
	fun findAllByAuthorId(authorId: Long): List<CommentDto> {
		return commentRepository.findAllByAuthor_Id(authorId)
			.map { CommentDto.fromEntity(it) }
	}

	/**
	 * 특정 사용자(author)가 작성한 모든 댓글 조회
	 * Entity -> DTO 변환
	 */
	fun findAllByAuthor(author: Member): List<CommentDto> {
		return commentRepository.findAllByAuthor(author)
			.map { CommentDto.fromEntity(it) }
	}


	/**
	 * 답글 생성
	 */
	fun createReplyComment(curationId: Long, commentId: Long, content: String): ReplyCommentDto {
		val curation = curationRepository.findById(curationId)
			.orElseThrow { ServiceException("404-1", "큐레이션이 존재하지 않습니다.") }

		val comment = commentRepository.findById(commentId)
			.orElseThrow { ServiceException("404-2", "댓글이 존재하지 않습니다.") }

		val reply = ReplyComment(
			author = rq.actor,
			comment = comment,
			curation = curation,
			content = content
		)

		val savedReply = replyCommentRepository.save(reply)
		return ReplyCommentDto.fromEntity(savedReply)
	}

	/**
	 * 답글 수정 권한 확인
	 */
	fun canEditReply(replyId: Long, userDetails: UserDetails?): Boolean {
		if (userDetails == null) throw ServiceException("401-1", "사용자 정보가 없습니다.")

		val reply = replyCommentRepository.findById(replyId)
			.orElseThrow { ServiceException("404-2", "해당 답글을 찾을 수 없습니다.") }

		if (userDetails !is SecurityUser) {
			throw ServiceException("401-3", "잘못된 인증 정보입니다.")
		}

		if (reply.author.id != userDetails.id) {
			throw ServiceException("403-2", "답글을 수정할 권한이 없습니다.")
		}

		return true
	}

	/**
	 * 답글 삭제 권한 확인
	 */
	fun canDeleteReply(replyId: Long, userDetails: UserDetails?): Boolean {
		if (userDetails == null) throw ServiceException("401-1", "사용자 정보가 없습니다.")

		val reply = replyCommentRepository.findById(replyId)
			.orElseThrow { ServiceException("404-2", "해당 답글을 찾을 수 없습니다.") }

		if (reply.author.id.toString() != userDetails.username) {
			throw ServiceException("403-2", "답글을 삭제할 권한이 없습니다.")
		}
		return true
	}

	/**
	 * 답글 내용 수정
	 */
	fun updateReply(replyId: Long, replyDto: CommentDto): ReplyCommentDto {
		val reply = replyCommentRepository.findById(replyId)
			.orElseThrow { ServiceException("404-2", "해당 댓글을 찾을 수 없습니다.") }

		reply.updateContent(replyDto.content)
		return ReplyCommentDto.fromEntity(reply)
	}

	/**
	 * 답글 삭제
	 */
	fun deleteReply(replyId: Long) {
		val reply = replyCommentRepository.findById(replyId)
			.orElseThrow { ServiceException("404-2", "해당 답글을 찾을 수 없습니다.") }

		replyCommentRepository.delete(reply)
	}
}
