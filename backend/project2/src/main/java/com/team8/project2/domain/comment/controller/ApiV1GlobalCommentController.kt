package com.team8.project2.domain.comment.controller

import com.team8.project2.domain.comment.dto.CommentDto
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import com.team8.project2.global.exception.ServiceException
import org.springframework.web.bind.annotation.*

/**
 * 전체 댓글 API 컨트롤러입니다.
 * 로그인된 사용자의 댓글 목록 조회 및 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/comments")
class ApiV1GlobalCommentController(
    private val memberService: MemberService,
    private val commentService: CommentService,
    private val curationService: CurationService,
    private val rq: Rq
) {

    /**
     * 로그인된 사용자의 댓글 목록을 조회합니다.
     * @return 댓글 DTO 리스트를 담은 응답 객체
     */
    @GetMapping("/mycomments")
    fun getCommentsByCurationId(): RsData<List<CommentDto>> {
        val author = rq.actor
        val member = memberService.findById(author.id)

        val commentDtos = commentService.findAllByAuthorId(member.id!!)
        return RsData.success("내 댓글 조회 성공", commentDtos)
    }

    /**
     * 특정 댓글을 삭제합니다.
     * @param commentId 삭제할 댓글의 ID
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/{id}")
    fun deleteComment(@PathVariable("id") commentId: Long): RsData<Unit> {
        val author = rq.actor
        commentService.deleteComment(commentId)
        return RsData.success("댓글이 삭제되었습니다.")
    }
}
