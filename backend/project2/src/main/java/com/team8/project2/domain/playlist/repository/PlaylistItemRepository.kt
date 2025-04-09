package com.team8.project2.domain.playlist.repository

import com.team8.project2.domain.playlist.entity.PlaylistItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlaylistItemRepository : JpaRepository<PlaylistItem, Long>