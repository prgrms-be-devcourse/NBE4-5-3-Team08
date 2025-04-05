package com.team8.project2.domain.playlist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team8.project2.domain.playlist.entity.PlaylistItem;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
}
