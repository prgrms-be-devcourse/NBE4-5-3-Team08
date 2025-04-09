package com.team8.project2.domain.playlist.dto

/**
 * 플레이리스트 수정 요청용 DTO 클래스입니다.
 * 클라이언트가 전송하는 변경 데이터를 검증하고 전달합니다.
 * null인 필드는 수정하지 않습니다.
 */
data class PlaylistUpdateDto (

    /** 변경할 플레이리스트 제목 */
    val title: String? = null,

    /** 변경할 플레이리스트 설명 */
    val description: String? = null,

    /** 플레이리스트 공개 여부 */
    val isPublic: Boolean? = null

)