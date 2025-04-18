package com.team8.project2.domain.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
class Follow {
    @EmbeddedId
    @Column(name = "followId")
    private var id: FollowId? = null

    @ManyToOne
    @JoinColumn(name = "followerId", insertable = false, updatable = false)
    private var follower: Member? = null

    @ManyToOne
    @JoinColumn(name = "followeeId", insertable = false, updatable = false)
    private var followee: Member? = null

    @CreatedDate
    @Column(updatable = false)
    var followedAt: LocalDateTime? = null

    @Embeddable
    class FollowId {
        var followerId: Long? = null
        var followeeId: Long? = null
    }

    fun setFollowerAndFollowee(follower: Member, followee: Member) {
        val followId = FollowId()
        followId.followerId = follower.id
        followId.followeeId = followee.id
        this.id = followId
        this.follower = follower
        this.followee = followee
    }

    val followeeName: String
        get() = followee?.getUsername() ?: "anonymous"

    val followeeProfileImage: String
        get() = followee?.profileImage ?: "fail profile"
}

