package com.team8.project2.domain.playlist.dto

import jakarta.validation.constraints.NotBlank

/**
 * 플레이리스트 아이템 수정 요청용 DTO입니다.
 */
data class PlaylistItemUpdateDto (

    /** 아이템 제목 */
    @field:NotBlank(message = "제목은 필수입니다.")
    val title: String,

    /** 아이템 URL */
    @field:NotBlank(message = "url은 필수입니다.")
    val url: String,

    /** 아이템 설명 */
    val description: String? = null
)