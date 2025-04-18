package com.team8.project2.domain.member.dto

import com.team8.project2.domain.member.entity.Member

data class AllMemberResDto(
    val members: List<MemberResDTO>,
    val totalPages: Int,
    val totalElements: Long,
    val numberOfElements: Int,
    val size: Int
) {
    companion object {
        @JvmStatic
        fun of(
            members: List<Member?>,
            totalPages: Int,
            totalElements: Long,
            numberOfElements: Int,
            size: Int
        ): AllMemberResDto {
            return AllMemberResDto(
                members = members.mapNotNull { it?.let(MemberResDTO::fromEntity) },
                totalPages = totalPages,
                totalElements = totalElements,
                numberOfElements = numberOfElements,
                size = size
            )
        }
    }
}