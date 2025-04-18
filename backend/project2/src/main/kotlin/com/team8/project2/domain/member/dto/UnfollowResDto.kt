package com.team8.project2.domain.member.dto

import com.team8.project2.domain.member.entity.Follow

data class UnfollowResDto(val followee: String) {
    companion object {
        fun fromEntity(follow: Follow) = UnfollowResDto(follow.followeeName!!)
    }
}