package com.team8.project2.domain.comment.controller

import com.team8.project2.domain.comment.dto.CommentDto
import com.team8.project2.domain.comment.dto.ReplyCommentDto
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * 댓글(Comment) API 컨트롤러 클래스입니다.
 * 댓글 생성, 조회 및 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/curations/{curationId}/comments")
class ApiV1CommentController(
	private val commentService: CommentService,
	private val rq: Rq
) {

	/**
	 * 새로운 댓글을 생성합니다.
	 * @param commentDto 댓글 생성 요청 데이터
	 * @return 생성된 댓글 정보를 포함한 응답
	 */
	@PostMapping
	@PreAuthorize("isAuthenticated()")
	fun createComment(
		@PathVariable curationId: Long,
		@RequestBody commentDto: CommentDto
	): RsData<CommentDto> {
		val actor: Member = rq.actor
		val createdComment = commentService.createComment(actor, curationId, commentDto)
		return RsData.success("댓글이 작성되었습니다.", createdComment)
	}

	/**
	 * 댓글에 답글을 작성합니다.
	 */
	@PostMapping("/{id}/reply")
	@PreAuthorize("isAuthenticated()")
	fun createReplyComment(
		@PathVariable curationId: Long,
		@PathVariable("id") commentId: Long,
		@RequestBody replyDto: CommentDto
	): RsData<ReplyCommentDto> {
		val replyCommentDto = commentService.createReplyComment(curationId, commentId, replyDto.content)
		return RsData.success("댓글의 답글이 작성되었습니다.", replyCommentDto)
	}

	/**
	 * 특정 큐레이션에 속한 댓글 목록을 조회합니다.
	 */
	@GetMapping
	fun getCommentsByCurationId(
		@PathVariable curationId: Long
	): RsData<List<CommentDto>> {
		val comments = commentService.getCommentsByCurationId(curationId)
		return RsData.success("댓글이 조회되었습니다.", comments)
	}

	/**
	 * 특정 댓글을 수정합니다.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("@commentService.canEditComment(#commentId, #userDetails)")
	fun updateComment(
		@PathVariable("id") commentId: Long,
		@RequestBody commentDto: CommentDto,
		@AuthenticationPrincipal userDetails: UserDetails
	): RsData<CommentDto> {
		val updatedComment = commentService.updateComment(commentId, commentDto)
		return RsData.success("댓글이 수정되었습니다.", updatedComment)
	}

	/**
	 * 특정 답글을 수정합니다.
	 */
	@PutMapping("/{commentId}/reply/{id}")
	@PreAuthorize("@commentService.canEditReply(#replyId, #userDetails)")
	fun updateReply(
		@PathVariable commentId: Long,
		@PathVariable("id") replyId: Long,
		@RequestBody replyDto: CommentDto,
		@AuthenticationPrincipal userDetails: UserDetails
	): RsData<ReplyCommentDto> {
		val updatedReplyDto = commentService.updateReply(replyId, replyDto)
		return RsData.success("답글이 수정되었습니다.", updatedReplyDto)
	}

	/**
	 * 특정 댓글을 삭제합니다.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("@commentService.canDeleteComment(#commentId, #userDetails)")
	fun deleteComment(
		@PathVariable("id") commentId: Long,
		@AuthenticationPrincipal userDetails: UserDetails
	): RsData<Unit> {
		commentService.deleteComment(commentId)
		return RsData.success("댓글이 삭제되었습니다.")
	}

	/**
	 * 특정 답글을 삭제합니다.
	 */
	@DeleteMapping("/{commentId}/reply/{id}")
	@PreAuthorize("@commentService.canDeleteReply(#replyId, #userDetails)")
	fun deleteReply(
		@PathVariable commentId: Long,
		@PathVariable("id") replyId: Long,
		@AuthenticationPrincipal userDetails: UserDetails
	): RsData<Unit> {
		commentService.deleteReply(replyId)
		return RsData.success("답글이 삭제되었습니다.")
	}
}
