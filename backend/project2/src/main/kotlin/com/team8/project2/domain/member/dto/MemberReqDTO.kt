package com.team8.project2.domain.member.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MemberReqDTO(
    @field:NotBlank(message = "회원 ID는 필수 입력값입니다.")
    @field:Size(max = 100, message = "회원 ID는 최대 100자까지 입력 가능합니다.")
    @JsonProperty("loginId")
    val loginId: String?,

    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @field:Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    @JsonProperty("password")
    val password: String?,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 100, message = "사용자 이름은 최대 100자까지 입력 가능합니다.")
    @JsonProperty("username")
    val username: String? = null,

    @JsonProperty("profileImage")
    val profileImage: String? = null,

    @JsonProperty("introduce")
    val introduce: String? = null,

    @JsonProperty("role")
    val role: RoleEnum = RoleEnum.MEMBER
){
    fun toEntity(): Member {
        return Member(
            loginId = this.loginId.toString(),
            username = this.username,
            password = this.password.toString(),
            role = this.role,
            profileImage = this.profileImage,
            email = this.email,
            introduce = this.introduce
        )
    }
}