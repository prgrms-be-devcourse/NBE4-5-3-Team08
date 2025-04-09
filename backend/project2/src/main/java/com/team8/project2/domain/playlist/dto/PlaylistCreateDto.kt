package com.team8.project2.domain.playlist.dto

import jakarta.validation.constraints.NotBlank

/**
 * 플레이리스트 생성 요청을 위한 PlaylistCreateDto 클래스입니다.
 * 클라이언트가 전송하는 플레이리스트 데이터를 검증하고 전달합니다.
 */
data class PlaylistCreateDto(

    /** 플레이리스트 제목 (필수) */
    @field:NotBlank(message = "플레이리스트 제목은 필수 입력 사항입니다.")
    val title: String,

    /** 플레이리스트 설명 (필수) */
    @field:NotBlank(message = "플레이리스트 설명은 필수 입력 사항입니다.")
    val description: String,

    /** 플레이리스트 공개 여부 (기본값: true) */
    val isPublic: Boolean = true
)