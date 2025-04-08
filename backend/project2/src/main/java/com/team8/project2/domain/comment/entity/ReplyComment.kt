package com.team8.project2.domain.comment.entity

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 답글(ReplyComment) 엔티티 클래스입니다.
 * 각 답글은 특정 댓글(Comment)과 큐레이션(Curation)에 속하며, 작성자 정보를 포함합니다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
class ReplyComment(

    /**
     * 댓글 작성자의 사용자 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId", nullable = false)
    var author: Member,

    /**
     * 상위 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commentId", nullable = false)
    var comment: Comment,

    /**
     * 댓글이 속한 큐레이션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curation_id", nullable = false)
    var curation: Curation,

    /**
     * 댓글 내용 (텍스트 형태, 필수값)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String

) {
    /**
     * 답글의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * 답글 생성 시간 (수정 불가)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        private set

    /**
     * 답글 수정 시간
     */
    @LastModifiedDate
    @Column(nullable = false)
    var modifiedAt: LocalDateTime? = null
        private set

    fun getAuthorName(): String = author.username

    fun getAuthorProfileImageUrl(): String = author.profileImage

    fun updateContent(content: String) {
        this.content = content
    }

    fun getAuthorId(): Long = author.id
}
