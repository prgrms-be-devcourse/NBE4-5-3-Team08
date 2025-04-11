package com.team8.project2.domain.playlist.controller

import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.playlist.dto.*
import com.team8.project2.domain.playlist.entity.PlaylistItem
import com.team8.project2.domain.playlist.service.PlaylistService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import com.team8.project2.global.dto.RsData.Companion.success
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/playlists")
class ApiV1PlaylistController(
    val playlistService: PlaylistService,
    val rq: Rq,
    val redisTemplate: RedisTemplate<String, Any>,
    val linkService: LinkService
) {
    /** 플레이리스트를 생성합니다. */
    @PostMapping
    fun createPlaylist(@Valid @RequestBody request: PlaylistCreateDto): RsData<PlaylistDto> {
        val playlist = playlistService.createPlaylist(request)
        return success("플레이리스트가 생성되었습니다.", playlist)
    }

    /**
     * 특정 플레이리스트를 조회합니다.
     *
     * @param id 조회할 플레이리스트의 ID
     * @return 조회된 플레이리스트 정보
     */
    @GetMapping("/{id}")
    fun getPlaylist(
        @PathVariable id: Long,
        request: HttpServletRequest
    ): RsData<PlaylistDto>  {
        val playlist = playlistService.getPlaylist(id, request)
        return success("플레이리스트 조회 성공", playlist)
    }


    /**
     * 사용자의 모든 플레이리스트를 조회합니다.
     *
     * @return 플레이리스트 목록
     */
    @GetMapping
    fun getAllPlaylists(): RsData<List<PlaylistDto>> {
        val playlists = playlistService.getAllPlaylists()
        return success("플레이리스트 목록 조회 성공", playlists)
    }

    /**
     * 플레이리스트를 수정합니다.
     *
     * @param id      수정할 플레이리스트의 ID
     * @param request 수정할 플레이리스트 데이터
     * @return 수정된 플레이리스트 정보
     */
    @PatchMapping("/{id}")
    fun updatePlaylist(
        @PathVariable id: Long,
        @Valid @RequestBody request: PlaylistUpdateDto
    ): RsData<PlaylistDto> {
        val updatedPlaylist = playlistService.updatePlaylist(id, request)
        return success("플레이리스트가 수정되었습니다.", updatedPlaylist)
    }

    /**
     * 플레이리스트를 삭제합니다.
     *
     * @param id 삭제할 플레이리스트의 ID
     * @return 삭제 결과 (null 반환)
     */
    @DeleteMapping("/{id}")
    fun deletePlaylist(@PathVariable id: Long): RsData<Unit> {
        playlistService.deletePlaylist(id)
        return success("플레이리스트가 삭제되었습니다.", Unit)
    }

    /**
     * 플레이리스트에 링크를 추가합니다.
     *
     * @param id 플레이리스트의 ID
     * @param linkReqDTO 링크 추가 요청 DTO
     * @return 업데이트된 플레이리스트 정보
     */
    @PostMapping("/{id}/items/link")
    fun addLinkToPlaylist(
        @PathVariable("id") id: Long,
        @Valid @RequestBody linkReqDTO: LinkReqDTO
    ): RsData<PlaylistDto> {
        val link = linkService.addLink(linkReqDTO)
        val updatedPlaylist =
            playlistService.addPlaylistItem(id, link.id!!, PlaylistItem.PlaylistItemType.LINK)
        return success("플레이리스트에 링크가 추가되었습니다.", updatedPlaylist)
    }

    /**
     * 플레이리스트에 큐레이션을 추가합니다.
     *
     * @param id 플레이리스트의 ID
     * @param request 큐레이션 ID를 담은 요청
     * @return 업데이트된 플레이리스트 정보
     */
    @PostMapping("/{id}/items/curation")
    fun addCurationToPlaylist(
        @PathVariable id: Long,
        @RequestBody request: Map<String, String>
    ): RsData<PlaylistDto>  {
        val curationId = request["curationId"]?.toLongOrNull()
            ?: return RsData.fail("400-1", "curationId가 유효하지 않습니다.")
        val updatedPlaylist =
            playlistService.addPlaylistItem(id, curationId, PlaylistItem.PlaylistItemType.CURATION)
        return success("플레이리스트에 큐레이션이 추가되었습니다.", updatedPlaylist)
    }

    /**
     * 플레이리스트 아이템을 삭제합니다.
     *
     * @param id 플레이리스트의 ID
     * @param itemId 삭제할 아이템의 식별자
     * @return 삭제 결과 (null 반환)
     */
    @DeleteMapping("/{id}/items/{itemId}")
    fun deletePlaylistItem(
        @PathVariable id: Long,
        @PathVariable itemId: Long
    ): RsData<Unit> {
        playlistService.deletePlaylistItem(id, itemId)
        return success("플레이리스트 아이템이 삭제되었습니다.", Unit)
    }

    /**
     * 플레이리스트 아이템 순서를 변경합니다.
     *
     * @param id 플레이리스트의 ID
     * @param orderUpdates 변경된 순서대로 정렬된 아이템 ID 계층구조
     * @return 순서가 변경된 플레이리스트 정보
     */
    @PatchMapping("/{id}/items/order")
    fun updatePlaylistItemOrder(
        @PathVariable id: Long,
        @RequestBody orderUpdates: List<PlaylistItemOrderUpdateDto>
    ): RsData<PlaylistDto> {
        val updatedPlaylist =
            playlistService.updatePlaylistItemOrder(id, orderUpdates)
        return success("플레이리스트 아이템 순서가 변경되었습니다.", updatedPlaylist)
    }


    /**
     * 플레이리스트 좋아요 상태를 토글합니다.
     *
     * @param id 플레이리스트 ID
     * @return 처리 결과 메시지
     */
    @PostMapping("/{id}/like")
    fun likePlaylist(@PathVariable id: Long): RsData<Unit> {
        val memberId = rq.actor.id
        playlistService.likePlaylist(id, memberId!!)
        return success("좋아요 상태가 토글되었습니다.", Unit)
    }

    /**
     * 플레이리스트 좋아요 상태를 조회합니다.
     *
     * @param id 플레이리스트 ID
     * @return true/false
     */
    @GetMapping("/{id}/like/status")
    fun likeStatus(@PathVariable id: Long): RsData<Boolean> {
        if (!rq.isLogin) {
            return success("비로그인 상태입니다.", false)
        }
        val memberId = rq.actor.id
        val liked = playlistService.hasLikedPlaylist(id, memberId!!)
        return success("좋아요 상태 조회 성공", liked)
    }

    /**
     * 플레이리스트의 좋아요 개수를 조회합니다.
     *
     * @param id 플레이리스트 ID
     * @return 좋아요 개수
     */
    @GetMapping("/{id}/like/count")
    fun getLikeCount(@PathVariable id: Long): RsData<Long> {
        val likeCount = playlistService.getLikeCount(id)
        return success("좋아요 개수를 조회하였습니다.", likeCount)
    }

    /**
     * 추천 플레이리스트 목록을 조회합니다. (정렬 기능 포함)
     *
     * @param id 기준이 되는 플레이리스트 ID
     * @param sortType 정렬 방식 (기본값: combined)
     * @return 추천된 플레이리스트 리스트
     */
    @GetMapping("/{id}/recommendation")
    fun getRecommendedPlaylists(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "combined") sortType: String
    ): RsData<List<PlaylistDto>>  {
        val recommended = playlistService.recommendPlaylist(id, sortType)
        return success("추천 플레이리스트 목록을 조회하였습니다.", recommended)
    }

    /**
     * 전체 공개 플레이리스트를 조회합니다.
     *
     * @return 공개 플레이리스트 목록
     */
    @GetMapping("/explore")
    fun getAllPublicPlaylists(): RsData<List<PlaylistDto>> {
        val playlists = playlistService.getAllPublicPlaylists()
        return success("공개 플레이리스트 전체 조회를 하였습니다.", playlists)
    }

    /**
     * 공개된 플레이리스트를 복제하여 추가합니다.
     *
     * @param playlistId 복제할 플레이리스트 ID
     * @return 복제된 플레이리스트 정보
     */
    @PostMapping("/{id}")
    fun addPublicPlaylist(@PathVariable("id") playlistId: Long): RsData<PlaylistDto> {
        val playlistDto = playlistService.addPublicPlaylist(playlistId)
        return success("플레이리스트가 복제되었습니다.", playlistDto)
    }

    /**
     * 사용자가 좋아요한 플레이리스트 목록을 조회합니다.
     *
     * @return 좋아요한 플레이리스트 리스트
     */
    @GetMapping("/liked")
    fun getLikedPlaylists(): RsData<List<PlaylistDto>> {
        val memberId = rq.actor.id
        val likedPlaylists = playlistService.getLikedPlaylistsFromRedis(memberId!!)
        return success("좋아요한 플레이리스트 조회 성공", likedPlaylists)
    }

    /**
     * 플레이리스트 아이템 정보를 수정합니다.
     *
     * @param id 플레이리스트 ID
     * @param itemId 수정할 아이템 ID
     * @param updateDto 수정할 데이터 DTO
     * @return 수정된 플레이리스트 정보
     */
    @PatchMapping("/{id}/items/{itemId}")
    fun updatePlaylistItem(
        @PathVariable id: Long,
        @PathVariable itemId: Long,
        @RequestBody updateDto: PlaylistItemUpdateDto
    ): RsData<PlaylistDto> {
        val updatedPlaylist = playlistService.updatePlaylistItem(id, itemId, updateDto)
        return success("플레이리스트 링크가 수정되었습니다.", updatedPlaylist)
    }
}