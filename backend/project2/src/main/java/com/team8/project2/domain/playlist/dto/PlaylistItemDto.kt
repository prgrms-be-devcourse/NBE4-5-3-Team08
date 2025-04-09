package com.team8.project2.domain.playlist.dto

import com.team8.project2.domain.playlist.entity.PlaylistItem

/**
 * 플레이리스트 내 개별 아이템 정보를 담는 DTO 클래스 입니다.
 * 아이템은 링크(Link) 또는 큐레이션(Curation) 중 하나일 수 있습니다.
 */
data class PlaylistItemDto(
    /** 플레이리스트 아이템 ID (PlaylistItem 자체의 ID) */
    val id: Long?,

    /** 참조 대상 ID (LINK ID 또는 CURATION ID) */
    val itemId: Long,

    /** 아이템 유형 (LINK 또는 CURATION) */
    val itemType: String,

    /** 링크 제목 */
    val title: String,

    /** 링크 설명 */
    val description: String,

    /** 링크 URL */
    val url: String,

    /** 큐레이션 ID (nullable) */
    val curationId: Long?,

    /** 부모 아이템 ID (nullable) */
    val parentItemId: Long?
) {
    companion object {
        /**
         * PlaylistItem 엔티티를 PlaylistItemDto로 변환합니다.
         * @param playlistItem 변환할 플레이리스트 아이템 엔티티
         * @return 변환된 PlaylistItemDto
         */
        fun fromEntity(playlistItem: PlaylistItem): PlaylistItemDto {
            val link = playlistItem.link
            val curation = playlistItem.curation

            return PlaylistItemDto(
                id = playlistItem.id,
                itemId = playlistItem.itemId,
                itemType = playlistItem.itemType.name,
                title = link?.title.orEmpty(),
                description = link?.description.orEmpty(),
                url = link?.url.orEmpty(),
                curationId = curation?.id,
                parentItemId = playlistItem.parentItemId
            )
        }
    }
}