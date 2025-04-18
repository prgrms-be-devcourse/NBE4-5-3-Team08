package com.team8.project2.domain.playlist.entity

import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.*
import java.io.Serializable

/**
 * 플레이리스트 좋아요(PlaylistLike) 엔티티 클래스입니다.
 * 사용자가 플레이리스트에 좋아요를 누른 정보를 저장합니다.
 */

@Entity
@Table(name = "playlist_likes")
class PlaylistLike(

    /**
     * 복합 키 (플레이리스트 ID + 멤버 ID)
     */
    @EmbeddedId
    var id: PlaylistLikeId = PlaylistLikeId(),

    /**
     * 좋아요한 플레이리스트 (N:1 관계, 읽기 전용)
     */
    @ManyToOne
    @JoinColumn(name = "playlistId", insertable = false, updatable = false)
    var playlist: Playlist,

    /**
     * 좋아요를 누른 사용자 (N:1 관계, 읽기 전용)
     */
    @ManyToOne
    @JoinColumn(name = "loginId", insertable = false, updatable = false)
    var member: Member

)  {
    /**
     * PlaylistLike 인스턴스 정적 생성 메서드
     * @param playlist 좋아요 대상 플레이리스트
     * @param member 좋아요를 누른 사용자
     * @return 생성된 PlaylistLike 인스턴스
     */
    companion object {
        fun createLike(playlist: Playlist, member: Member): PlaylistLike {
            val likeId = PlaylistLikeId(
                playlistId = playlist.id,
                loginId = member.id
            )
            return PlaylistLike(
                id = likeId,
                playlist = playlist,
                member = member
            )
        }
    }

    /**
     * 플레이리스트 좋아요의 복합 키 클래스
     * Playlist ID + Member ID로 구성되며, JPA에서 식별자 역할
     */
    @Embeddable
    data class PlaylistLikeId(
        var playlistId: Long? = null,
        var loginId: Long? = null
    ) : Serializable
}