package com.team8.project2.domain.playlist.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.playlist.entity.Playlist
import java.time.LocalDateTime

/**
 * 플레이리스트 데이터를 전송하는 DTO 클래스입니다.
 * 엔티티를 DTO로 변환하여 클라이언트에 전달할 데이터를 구성합니다.
 */
data class PlaylistDto(
    /** 플레이리스트 ID */
    val id: Long,

    /** 제목 */
    val title: String,

    /** 설명 */
    val description: String,

    /** 공개 여부 */
    @JsonProperty("isPublic")
    val isPublic: Boolean,

    /** 조회 수 */
    val viewCount: Long,

    /** 좋아요 수 */
    val likeCount: Long,

    /** 포함된 항목 리스트 */
    val items: List<PlaylistItemDto> = emptyList(),

    /** 태그 목록 */
    val tags: Set<String> = emptySet(),

    /** 생성일 */
    val createdAt: LocalDateTime,

    /** 현재 요청한 사용자가 소유자인지 여부 */
    val isOwner: Boolean

) {
    companion object {
        /**
         * Playlist 엔티티를 PlaylistDto로 변환합니다.
         * @param playlist 변환할 엔티티
         * @param actor 현재 로그인한 사용자 (nullable)
         * @return 변환된 PlaylistDto
         */
        fun fromEntity(playlist: Playlist, actor: Member?): PlaylistDto {
            val isOwner = actor?.id == playlist.member.id
            val sortedItems = playlist.items
                .sortedBy { it.displayOrder }
                .map { PlaylistItemDto.fromEntity(it) }
            val tagNames = playlist.tags.map { it.name }.toSet()

            return PlaylistDto(
                id = requireNotNull(playlist.id) { "Playlist ID null 안됨" },
                title = playlist.title,
                description = playlist.description,
                isPublic = playlist.isPublic,
                viewCount = playlist.viewCount,
                likeCount = playlist.likeCount,
                items = sortedItems,
                tags = tagNames,
                createdAt = requireNotNull(playlist.createdAt) { "Playlist createdAt null 안됨" },
                isOwner = isOwner
            )

        }
    }

}