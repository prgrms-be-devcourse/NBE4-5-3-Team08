package com.team8.project2.domain.playlist.repository

import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.playlist.entity.Playlist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PlaylistRepository : JpaRepository<Playlist, Long> {

    @Query(
        """
        SELECT DISTINCT p FROM Playlist p JOIN p.tags t
        WHERE t IN :tags AND p.id <> :playlistId
        """
    )
    fun findByTags(
        @Param("tags") tags: Set<Tag>,
        @Param("playlistId") playlistId: Long
    ): List<Playlist>

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM Playlist p")
    fun sumTotalViews(): Long

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Playlist p")
    fun sumTotalLikes(): Long

    fun findByMember(member: Member): List<Playlist>

    fun findAllByIsPublicTrue(): List<Playlist>

    @Query(
        """
        SELECT DISTINCT p FROM Playlist p JOIN p.items pi
        WHERE pi.curation.id = :curationId AND p.member = :member
        """
    )
    fun findByMemberAndCuration(
        @Param("member") member: Member,
        @Param("curationId") curationId: Long
    ): List<Playlist>
}