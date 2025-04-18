package com.team8.project2.domain.member.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.global.exception.ServiceException
import lombok.Data
import java.time.LocalDateTime

data class MemberResDTO(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("loginId")
    val loginId: String?,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("email")
    val email: String?,

    @JsonProperty("role")
    val role: RoleEnum,

    @JsonProperty("profileImage")
    val profileImage: String?,

    @JsonProperty("introduce")
    val introduce: String?,

    @JsonProperty("createdDatetime")
    val createdDate: LocalDateTime?,

    @JsonProperty("modifiedDatetime")
    val modifiedDate: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): MemberResDTO {
            return MemberResDTO(
                id = member.id?: throw ServiceException("400-1", "ID는 null일 수 없습니다."),
                loginId = member.getLoginId(),
                username = member.getUsername(),
                email = member.email,
                role = member.role,
                profileImage = member.profileImage,
                introduce = member.introduce,
                createdDate = member.createdDate,
                modifiedDate = member.modifiedDate
            )
        }
    }
}