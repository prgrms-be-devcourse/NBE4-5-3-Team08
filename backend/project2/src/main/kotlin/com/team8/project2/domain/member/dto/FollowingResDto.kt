package com.team8.project2.domain.member.dto

import com.team8.project2.domain.member.dto.FollowResDto.Companion.fromEntity
import com.team8.project2.domain.member.entity.Follow
import lombok.Getter

@Getter
class FollowingResDto {
    var following: MutableList<FollowResDto> = ArrayList()

    companion object {
        fun fromEntity(followings: List<Follow?>): FollowingResDto {
            val dto = FollowingResDto()
            for (follow in followings) {
                dto.following.add(fromEntity(follow!!))
            }
            return dto
        }
    }
}
