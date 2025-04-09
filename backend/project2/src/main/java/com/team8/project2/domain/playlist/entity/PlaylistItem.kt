package com.team8.project2.domain.playlist.entity

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.link.entity.Link
import jakarta.persistence.*

/**
 * 플레이리스트 항목(PlaylistItem) 엔티티 클래스입니다.
 * 플레이리스트에 포함된 개별 항목을 저장합니다.
 */

@Entity
class PlaylistItem(

    /**
     * 플레이리스트 항목의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 항목 고유 ID (예: 링크 ID 또는 큐레이션 ID)
     */
    @Column(nullable = false)
    var itemId: Long,

    /**
     * 부모 아이템 ID (nullable)
     */
    @Column(name = "parent_item_id")
    var parentItemId: Long? = null,

    /**
     * 항목 유형 (LINK 또는 CURATION)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var itemType: PlaylistItemType,

    /**
     * 해당 항목이 속한 플레이리스트 (N:1 관계, 필수값)
     */
    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false)
    var playlist: Playlist,

    /**
     * 해당 항목이 속한 큐레이션 (N:1 관계, nullable)
     */
    @ManyToOne
    @JoinColumn(name = "curation_id")
    var curation: Curation? = null,

    /**
     * 아이템 순서
     */
    @Column(nullable = false)
    var displayOrder: Int,

    /**
     * 해당 항목이 참조하는 링크 (조회 전용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId", referencedColumnName = "linkId", insertable = false, updatable = false)
    var link: Link? = null

) {
    enum class PlaylistItemType {
        LINK,
        CURATION
    }

    // 임시 생성자 추가
    constructor(
        itemId: Long,
        itemType: PlaylistItemType,
        playlist: Playlist,
        curation: Curation?,
        parentItemId: Long?,
        displayOrder: Int
    ) : this(
        id = null,
        itemId = itemId,
        parentItemId = parentItemId,
        itemType = itemType,
        playlist = playlist,
        curation = curation,
        displayOrder = displayOrder,
        link = null
    )

}