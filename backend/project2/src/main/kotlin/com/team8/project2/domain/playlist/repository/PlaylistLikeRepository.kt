package com.team8.project2.domain.playlist.repository

import com.team8.project2.domain.playlist.entity.PlaylistLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlaylistLikeRepository : JpaRepository<PlaylistLike, PlaylistLike.PlaylistLikeId> {
    /** 특정 플레이리스트의 좋아요 확인  */
    fun existsById_PlaylistId(playlistId: Long): Boolean

    /** 특정 플레이리스트의 좋아요 삭제  */
    fun deleteById_PlaylistId(playlistId: Long)

    /** 특정 사용자의 모든 좋아요 데이터 조회  */
    fun findByIdLoginId(loginId: Long): List<PlaylistLike>

    /** 특정 플레이리스트의 모든 좋아요 데이터 조회  */
    fun findAllById_PlaylistId(playlistId: Long): List<PlaylistLike>
}
