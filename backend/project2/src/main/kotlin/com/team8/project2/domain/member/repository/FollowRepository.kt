package com.team8.project2.domain.member.repository

import com.team8.project2.domain.member.entity.Follow
import com.team8.project2.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FollowRepository : JpaRepository<Follow, Long> {
    fun findByFollowerAndFollowee(follower: Member?, followee: Member?): Follow?

    fun findByFollower(actor: Member): List<Follow?>?

    fun existsByFollowerIdAndFolloweeId(followerId: Long, followeeId: Long): Boolean

    fun deleteByFollowerOrFollowee(member: Member, member2: Member)
}
