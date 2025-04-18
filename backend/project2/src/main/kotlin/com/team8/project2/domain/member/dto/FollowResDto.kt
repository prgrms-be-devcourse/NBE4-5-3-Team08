package com.team8.project2.domain.member.dto

import com.team8.project2.domain.member.entity.Follow
import java.time.LocalDateTime

class FollowResDto(
    val followee: String?,
    val profileImage: String?,
    val followedAt: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun fromEntity(follow: Follow): FollowResDto {
            return FollowResDto(
                follow.followeeName,
                follow.followeeProfileImage,
                follow.followedAt
            )
        }
    }
}