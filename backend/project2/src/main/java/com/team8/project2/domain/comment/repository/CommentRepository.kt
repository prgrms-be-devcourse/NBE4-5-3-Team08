package com.team8.project2.domain.comment.repository

import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository


/**
 * 댓글(Comment) 엔티티에 대한 데이터 접근 레이어(Repository)입니다.
 * JPA를 사용하여 데이터베이스 연산을 수행합니다.
 */
interface CommentRepository : JpaRepository<Comment, Long> {

    /**
     * 특정 큐레이션 ID에 해당하는 모든 댓글을 조회합니다.
     * @param curationId 큐레이션 ID
     * @return 해당 큐레이션에 속한 댓글 리스트
     */
    fun findByCurationId(curationId: Long): List<Comment>

    /**
     * 특정 회원 ID가 작성한 모든 댓글을 조회합니다.
     * @param memberId 회원 ID
     */
    fun findAllByAuthor_Id(memberId: Long): List<Comment>

    /**
     * 특정 회원이 작성한 모든 댓글을 조회합니다.
     * @param author 회원 객체
     */
    fun findAllByAuthor(author: Member): List<Comment>

    /**
     * 특정 회원이 작성한 댓글을 모두 삭제합니다.
     * @param member 댓글 작성자
     */
    fun deleteByAuthor(member: Member)
}
