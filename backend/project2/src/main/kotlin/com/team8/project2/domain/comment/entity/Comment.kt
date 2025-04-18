package com.team8.project2.domain.comment.entity

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 댓글(Comment) 엔티티 클래스입니다.
 * 각 댓글은 특정 큐레이션(Curation)과 연관되며, 작성자 ID 및 내용 정보를 포함합니다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
class Comment(

    /**
     * 댓글 작성자의 사용자 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loginId", nullable = false)
    val author: Member,

    /**
     * 댓글이 속한 큐레이션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curation_id", nullable = false)
    val curation: Curation,

    /**
     * 댓글 내용 (텍스트 형태, 필수값)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String

) {
    /**
     * 댓글의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * 댓글에 달린 답글 목록
     */
    @OneToMany(mappedBy = "comment", orphanRemoval = true)
    val replyComments: MutableList<ReplyComment> = mutableListOf()

    /**
     * 댓글 생성 시간 (수정 불가)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    /**
     * 댓글 수정 시간
     */
    @LastModifiedDate
    @Column(nullable = false)
    var modifiedAt: LocalDateTime? = null

    fun addReplyComment(replyComment: ReplyComment) {
        this.replyComments.add(replyComment)
    }

    fun getAuthorName(): String = author.getUsername()

    fun getAuthorId(): Long = author.id!!

    fun getAuthorImgUrl(): String = author.profileImage.toString()

    fun updateContent(content: String) {
        this.content = content
    }
}