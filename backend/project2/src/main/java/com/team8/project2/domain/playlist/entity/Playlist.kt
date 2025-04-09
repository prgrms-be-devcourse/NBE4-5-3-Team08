package com.team8.project2.domain.playlist.entity

import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/** 사용자가 생성한 플레이리스트 정보를 나타내는 Playlist 엔티티입니다. */

@Entity
@EntityListeners(AuditingEntityListener::class)
class Playlist(

    /** 플레이리스트의 고유 ID (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** 제목 (필수값) */
    @Column(nullable = false)
    var title: String,

    /** 설명 (필수값) */
    @Column(nullable = false)
    var description: String,

    /** 공개 여부 (기본값: 공개) */
    @Column(nullable = false)
    var isPublic: Boolean = true,

    /** 조회수 */
    @Column(nullable = false)
    var viewCount: Long = 0L,

    /** 좋아요 수 */
    @Column(nullable = false)
    var likeCount: Long = 0L,

    /** 플레이리스트에 포함된 항목 목록 (1:N 관계) */
    @OneToMany(mappedBy = "playlist", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    var items: MutableList<PlaylistItem> = mutableListOf(),

    /** 플레이리스트 연관 추천 태그 */
    @ManyToMany
    @JoinTable(
        name = "PlaylistTag",
        joinColumns = [JoinColumn(name = "playlist_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    var tags: MutableSet<Tag> = mutableSetOf(),

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    var modifiedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member
) {
    /**
     * 플레이리스트 정보를 수정하는 메서드
     * @param title 변경할 제목 (null일 경우 변경 없음)
     * @param description 변경할 설명 (null일 경우 변경 없음)
     * @param isPublic 변경할 공개 여부 (null일 경우 변경 없음)
     */
    fun updatePlaylist(title: String?, description: String?, isPublic: Boolean?) {
        title?.let { this.title = it }
        description?.let { this.description = it }
        isPublic?.let { this.isPublic = it }
    }

    /**  태그 이름을 문자열 Set으로 반환 */
    val tagNames: Set<String>
        get() = tags.map { it.name }.toSet()

    /** 좋아요 수 갱신 */
    fun updateLikeCount(newLikeCount: Long) {
        this.likeCount = newLikeCount
    }
}