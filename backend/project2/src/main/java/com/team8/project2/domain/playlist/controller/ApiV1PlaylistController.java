package com.team8.project2.domain.playlist.controller;

import com.team8.project2.domain.link.dto.LinkReqDTO;
import com.team8.project2.domain.link.service.LinkService;
import com.team8.project2.domain.playlist.dto.*;
import com.team8.project2.domain.playlist.entity.PlaylistItem;
import com.team8.project2.domain.playlist.repository.PlaylistLikeRepository;
import com.team8.project2.domain.playlist.repository.PlaylistRepository;
import com.team8.project2.domain.playlist.service.PlaylistService;
import com.team8.project2.global.Rq;
import com.team8.project2.global.dto.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 플레이리스트(Playlist) API 컨트롤러 클래스입니다.
 * <p>
 * 플레이리스트 생성, 조회, 수정, 삭제 등의 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class ApiV1PlaylistController {

    private final PlaylistService playlistService;
    private final PlaylistRepository playlistRepository;
    private final PlaylistLikeRepository playlistLikeRepository;
    private final Rq rq;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 플레이리스트를 생성합니다.
     *
     * @param request 플레이리스트 생성 요청 DTO
     * @return 생성된 플레이리스트 정보
     */
    @PostMapping
    public RsData<PlaylistDto> createPlaylist(@Valid @RequestBody PlaylistCreateDto request) {
        PlaylistDto playlist = playlistService.createPlaylist(request);
        return RsData.success("플레이리스트가 생성되었습니다.", playlist);
    }

    /**
     * 특정 플레이리스트를 조회합니다.
     *
     * @param id 조회할 플레이리스트의 ID
     * @return 조회된 플레이리스트 정보
     */
    @GetMapping("/{id}")
    public RsData<PlaylistDto> getPlaylist(@PathVariable Long id, HttpServletRequest request) {
        PlaylistDto playlist = playlistService.getPlaylist(id, request);
        return RsData.success("플레이리스트 조회 성공", playlist);
    }


    /**
     * 사용자의 모든 플레이리스트를 조회합니다.
     *
     * @return 플레이리스트 목록
     */
    @GetMapping
    public RsData<List<PlaylistDto>> getAllPlaylists() {
        List<PlaylistDto> playlists = playlistService.getAllPlaylists();
        return RsData.success("플레이리스트 목록 조회 성공", playlists);
    }

    /**
     * 플레이리스트를 수정합니다.
     *
     * @param id      수정할 플레이리스트의 ID
     * @param request 수정할 플레이리스트 데이터
     * @return 수정된 플레이리스트 정보
     */
    @PatchMapping("/{id}")
    public RsData<PlaylistDto> updatePlaylist(
            @PathVariable Long id,
            @Valid @RequestBody PlaylistUpdateDto request
    ) {
        PlaylistDto updatedPlaylist = playlistService.updatePlaylist(id, request);
        return RsData.success("플레이리스트가 수정되었습니다.", updatedPlaylist);
    }

    /**
     * 플레이리스트를 삭제합니다.
     *
     * @param id 삭제할 플레이리스트의 ID
     * @return 삭제 결과 (null 반환)
     */
    @DeleteMapping("/{id}")
    public RsData<Void> deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return RsData.success("플레이리스트가 삭제되었습니다.", null);
    }

    /**
     * 플레이리스트에 링크를 추가합니다.
     *
     * @param id      플레이리스트의 ID
     * @param linkReqDTO 링크 추가 요청 DTO
     * @return 업데이트된 플레이리스트 정보
     */
    @PostMapping("/{id}/items/link")
    public RsData<PlaylistDto> addLinkToPlaylist(
            @PathVariable("id") Long id,
            @RequestBody @Valid LinkReqDTO linkReqDTO
    ) {
        Link link = linkService.addLink(linkReqDTO);
        PlaylistDto updatedPlaylist =
                playlistService.addPlaylistItem(id, link.getId(), PlaylistItem.PlaylistItemType.LINK);
        return RsData.success("플레이리스트에 링크가 추가되었습니다.", updatedPlaylist);
    }

    /**
     * 플레이리스트에 큐레이션을 추가합니다.
     *
     * @param id      플레이리스트의 ID
     * @param request 큐레이션 ID를 담은 요청
     * @return 업데이트된 플레이리스트 정보
     */
    @PostMapping("/{id}/items/curation")
    public RsData<PlaylistDto> addCurationToPlaylist(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request
    ) {
        Long curationId = Long.parseLong(request.get("curationId"));
        PlaylistDto updatedPlaylist =
                playlistService.addPlaylistItem(id, curationId, PlaylistItem.PlaylistItemType.CURATION);
        return RsData.success("플레이리스트에 큐레이션이 추가되었습니다.", updatedPlaylist);
    }

    /**
     * 플레이리스트 아이템을 삭제합니다.
     *
     * @param id     플레이리스트의 ID
     * @param itemId 삭제할 아이템의 식별자
     * @return 삭제 결과 (null 반환)
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public RsData<Void> deletePlaylistItem(
            @PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId
    ) {
        playlistService.deletePlaylistItem(id, itemId);
        return RsData.success("플레이리스트 아이템이 삭제되었습니다.", null);
    }

    /**
     * 플레이리스트 아이템 순서를 변경합니다.
     *
     * @param id             플레이리스트의 ID
     * @param orderUpdates 변경된 순서대로 정렬된 아이템 ID 계층구조
     * @return 순서가 변경된 플레이리스트 정보
     */
    @PatchMapping("/{id}/items/order")
    public RsData<PlaylistDto> updatePlaylistItemOrder(
            @PathVariable("id") Long id,
            @RequestBody List<PlaylistItemOrderUpdateDto> orderUpdates
    ) {
        PlaylistDto updatedPlaylist =
                playlistService.updatePlaylistItemOrder(id, orderUpdates);
        return RsData.success("플레이리스트 아이템 순서가 변경되었습니다.", updatedPlaylist);
    }

    /** ✅ 좋아요 증가 API */
    @PostMapping("/{id}/like")
    public RsData<Void> likePlaylist(@PathVariable Long id) {
        Long memberId = rq.getActor().getId();
        playlistService.likePlaylist(id, memberId);
        return RsData.success("좋아요 상태가 토글되었습니다.", null);
    }

    /** ✅ 좋아요 상태 조회 API */
     @GetMapping("/{id}/like/status")
    public RsData<Boolean> likeStatus(@PathVariable Long id) {
         if (!rq.isLogin()) {
             return RsData.success("비로그인 상태입니다.", false);
         }
         Long memberId = rq.getActor().getId();
         boolean liked = playlistService.hasLikedPlaylist(id, memberId);
         return RsData.success("좋아요 상태 조회 성공", liked);
    }


    /** ✅ 좋아요 개수 조회 API */
    @GetMapping("/{id}/like/count")
    public RsData<Long> getLikeCount(@PathVariable Long id) {
        long likeCount = playlistService.getLikeCount(id);
        return RsData.success("좋아요 개수를 조회하였습니다.", likeCount);
    }

    /** ✅ 추천 API (정렬 기능 추가) */
    @GetMapping("/{id}/recommendation")
    public RsData<List<PlaylistDto>> getRecommendedPlaylists(
            @PathVariable Long id,
            @RequestParam(defaultValue = "combined") String sortType
    ) {
        List<PlaylistDto> recommended = playlistService.recommendPlaylist(id, sortType);
        return RsData.success("추천 플레이리스트 목록을 조회하였습니다.", recommended);
    }

    private final LinkService linkService;

    @GetMapping("/explore")
    public RsData<List<PlaylistDto>> getAllPublicPlaylists(){
        List<PlaylistDto> playlists = playlistService.getAllPublicPlaylists();
        return RsData.success("공개 플레이리스트 전체 조회를 하였습니다.", playlists);
    }

    @PostMapping("/{id}")
    public RsData<PlaylistDto> addPublicPlaylist(@PathVariable(name = "id") Long playlistId) {
        PlaylistDto playlistDto = playlistService.addPublicPlaylist(playlistId);
        return RsData.success("플레이리스트가 복제되었습니다.", playlistDto);
    }

    /** 좋아요한 플레이리스트 조회 API */
    @GetMapping("/liked")
    public RsData<List<PlaylistDto>> getLikedPlaylists() {
        Long memberId = rq.getActor().getId();
        List<PlaylistDto> likedPlaylists = playlistService.getLikedPlaylistsFromRedis(memberId);
        return RsData.success("좋아요한 플레이리스트 조회 성공", likedPlaylists);
    }

    /** 플레이리스트 아이템 정보 수정 API */
    @PatchMapping("/{id}/items/{itemId}")
    public RsData<PlaylistDto> updatePlaylistItem(
            @PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId,
            @RequestBody PlaylistItemUpdateDto updateDto
    ) {
        PlaylistDto updatePlaylistItem = playlistService.updatePlaylistItem(id, itemId, updateDto);
        return RsData.success("플레이리스트 링크가 수정되었습니다.", updatePlaylistItem);
    }
}
