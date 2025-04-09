package com.team8.project2.domain.playlist.dto

/**
 * 플레이리스트 아이템 순서 변경 요청용 DTO입니다.
 * 메인 아이템 ID와 큐레이션 그룹일 경우, 하위 아이템 ID 목록을 포함합니다.
 */
data class PlaylistItemOrderUpdateDto(
    /** 메인 아이템 또는 그룹 헤더의 ID */
    val id: Long,

    /** 그룹 내 아이템의 순서를 나타내는 ID */
    val children: List<Long> = emptyList()
)