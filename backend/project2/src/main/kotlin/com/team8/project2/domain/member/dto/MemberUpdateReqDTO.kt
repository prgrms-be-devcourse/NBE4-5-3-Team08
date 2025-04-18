package com.team8.project2.domain.member.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

data class MemberUpdateReqDTO(
    @field:NotBlank(message = "회원 ID는 필수 입력값입니다.")
    @field:Size(max = 100, message = "회원 ID는 최대 100자까지 입력 가능합니다.")
    @JsonProperty("loginId")
    val loginId: String? = null,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 100, message = "사용자 이름은 최대 100자까지 입력 가능합니다.")
    @JsonProperty("username")
    val username: String? = null,

    @JsonProperty("profileImage")
    val profileImage: String? = null,

    @JsonProperty("introduce")
    val introduce: String? = null
)
