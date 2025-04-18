package com.team8.project2.domain.admin.dto

/**
 * 큐레이션 및 플레이리스트의 통계 응답 DTO입니다.
 * 조회수 및 좋아요 수를 클라이언트에 전달합니다.
 */
data class StatsResDto(

    /** 전체 큐레이션 조회수 */
    val totalCurationViews: Long,

    /** 전체 큐레이션 좋아요 수 */
    val totalCurationLikes: Long,

    /** 전체 플레이리스트 조회수 */
    val totalPlaylistViews: Long,

    /** 전체 플레이리스트 좋아요 수 */
    val totalPlaylistLikes: Long
)