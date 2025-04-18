package com.team8.project2.domain.link.dto

import jakarta.validation.constraints.NotNull
/**
 * 링크 생성 및 수정 요청을 위한 DTO 클래스입니다.
 * 클라이언트가 전송하는 링크 데이터를 검증하고 전달합니다.
 */
data class LinkReqDTO(
    /**
     * 링크 URL (필수값)
     */
    @field:NotNull
    val url: String,

    /**
     * 링크 TITLE (필수값 아님)
     */
    val title: String? = null,

    val description: String? = null
) {
    constructor(url: String) : this(url, null, null) // Java 통합을 위해 임시로 생성
}
