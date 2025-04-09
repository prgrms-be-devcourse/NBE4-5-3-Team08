package com.team8.project2.domain.playlist.entity;

import com.team8.project2.domain.curation.curation.entity.Curation;
import jakarta.persistence.*;
import lombok.*;

/**
 * 플레이리스트 항목(PlaylistItem) 엔티티 클래스입니다.
 * 플레이리스트에 포함된 개별 항목을 저장합니다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistItem {

    /**
     * 플레이리스트 항목의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 항목의 고유 ID (예: 음악, 영상 등)
     */
    @Column(nullable = false) // NULL 허용 안 함
    private Long itemId;

    /**
     * 부모 아이템 ID (null일 경우 최상위 플레이리스트)
     */
    @Column(name = "parent_item_id", nullable = true)
    private Long parentItemId;


    /**
     * 항목의 유형 (예: 음악, 영상 등)
     */
    @Column(nullable = false) // NULL 허용 안 함
    @Enumerated(EnumType.STRING)
    private PlaylistItemType itemType;

    /**
     * 해당 항목이 속한 플레이리스트 (N:1 관계)
     */
    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false) // NULL 허용 안 함 (반드시 Playlist와 연결)
    private Playlist playlist;

    /**
     * 해당 항목이 속한 큐레이션 (N:1 관계)
     */
    @ManyToOne
    @JoinColumn(name = "curation_id", nullable = true) // NULL 허용
    private Curation curation;

    /**
     * 아이템 순서
     */
    @Column(nullable = false)
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId", referencedColumnName = "linkId", insertable = false, updatable = false)
    private Link link;

    public enum PlaylistItemType {
        LINK,
        CURATION
    }


}
