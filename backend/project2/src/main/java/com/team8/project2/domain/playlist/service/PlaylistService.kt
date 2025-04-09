package com.team8.project2.domain.playlist.service

import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.playlist.dto.*
import com.team8.project2.domain.playlist.entity.Playlist
import com.team8.project2.domain.playlist.entity.PlaylistItem
import com.team8.project2.domain.playlist.entity.PlaylistLike
import com.team8.project2.domain.playlist.repository.PlaylistLikeRepository
import com.team8.project2.domain.playlist.repository.PlaylistRepository
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.BadRequestException
import com.team8.project2.global.exception.NotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration

/**
 * 플레이리스트(Playlist) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 플레이리스트 생성, 조회, 수정, 삭제 등의 로직을 처리합니다.
 */
@Service
@Transactional
class PlaylistService(
    private val playlistRepository: PlaylistRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val memberRepository: MemberRepository,
    private val playlistLikeRepository: PlaylistLikeRepository,
    private val rq: Rq,
    private val linkService: LinkService

) {
    companion object {
        private const val VIEW_COUNT_KEY = "playlist:view_count:"
        private const val LIKE_COUNT_KEY = "playlist:like_count:"
        private const val RECOMMEND_KEY = "playlist:recommend:"

    }


    /**
     * 현재 로그인한 사용자의 플레이리스트 목록을 조회합니다.
     *
     * @return 해당 사용자의 플레이리스트 DTO 리스트. 없으면 빈 리스트 반환.
     */
    fun getAllPlaylists(): List<PlaylistDto> {
        val actor = rq.actor
        val playlists = playlistRepository.findByMember(actor)

        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }

    /**
     * 공개된 모든 플레이리스트를 조회합니다.
     *
     *  @return 공개 플레이리스트 DTO 리스트
     */
    fun getAllPublicPlaylists(): List<PlaylistDto> {
        val playlists = playlistRepository.findAllByIsPublicTrue()
        val actor = if (rq.isLogin) rq.actor else null

        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }

    /**
     * 현재 로그인한 사용자의 플레이리스트를 생성합니다.
     *
     * @param request 플레이리스트 생성 요청 데이터
     * @return 생성된 플레이리스트 DTO
     */
    fun createPlaylist(request: PlaylistCreateDto): PlaylistDto {
        val actor = rq.actor

        return createPlaylist(request, actor)
    }

    /**
     * 특정 사용자의 플레이리스트를 생성합니다.
     *
     * @param request 플레이리스트 생성 요청 데이터
     * @param member 플레이리스트를 생성할 사용자
     * @return 생성된 플레이리스트 DTO
     */
    fun createPlaylist(request: PlaylistCreateDto, member: Member): PlaylistDto {
        val playlist = Playlist(
            title = request.title,
            description = request.description,
            isPublic = request.isPublic ?: true,
            member = member
        )
        return PlaylistDto.fromEntity(playlistRepository.save(playlist), member)
    }

    /**
     * 특정 플레이리스트를 조회합니다.
     * IP 기준으로 중복 조회를 방지하고, Redis에 조회수를 기록합니다.
     *
     * @param id 조회할 플레이리스트 ID
     * @param request 클라이언트 요청 (IP 확인용)
     * @return 조회된 플레이리스트 DTO
     */
    fun getPlaylist(id: Long, request: HttpServletRequest): PlaylistDto {
        val playlist = playlistRepository.findById(id)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val currentViewCount = redisTemplate.opsForZSet()
            .score(VIEW_COUNT_KEY, id.toString())
            ?.toLong() ?: 0L
        playlist.viewCount = currentViewCount

        if (playlist.items == null) {
            playlist.items = mutableListOf()
        }

        val ip = getClientIp(request)
        val redisKey = "playlist_view_${id}_$ip"
        val isNewView = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "true", Duration.ofDays(1))

        if (isNewView == true) {
            redisTemplate.opsForZSet().incrementScore(VIEW_COUNT_KEY, id.toString(), 1.0)
            playlist.viewCount = currentViewCount + 1
        }

        val actor = if (rq.isLogin) rq.actor else null
        return PlaylistDto.fromEntity(playlist, actor)
    }

    /**
     * 클라이언트의 IP 주소를 추출합니다.
     * 정확한 IP를 얻기 위해 여러 헤더를 순차적으로 검사합니다.
     *
     * @param request 클라이언트의 HTTP 요청
     * @return 추출된 IP 주소 문자열
     */
    private fun getClientIp(request: HttpServletRequest): String? {
        val headers = listOf(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP",
            "X-RealIP",
            "REMOTE_ADDR"
        )

        for (header in headers) {
            val ip = request.getHeader(header)
            if (!ip.isNullOrEmpty() && !ip.equals("unknown", ignoreCase = true)) {
                return ip
            }
        }

        val ip = request.remoteAddr

        return if (ip == "0:0:0:0:0:0:0:1" || ip == "127.0.0.1") {
            try {
                val address = InetAddress.getLocalHost()
                "${address.hostName}/${address.hostAddress}"
            } catch (e: UnknownHostException) {
                throw RuntimeException(e)
            }
        } else {
            ip
        }
    }

    /**
     * Redis에 저장된 플레이리스트 조회수를 DB에 동기화합니다.
     * Redis 키에서 ID를 추출하고, 해당 ID의 조회수를 DB에 반영합니다.
     */
    @Scheduled(fixedRate = 600000)
    fun syncViewCountsToDB() {
        val keys = redisTemplate.keys("$VIEW_COUNT_KEY*")

        keys?.forEach { key ->
            try {
                val idStr = key.removePrefix(VIEW_COUNT_KEY).trim()
                if (idStr.isEmpty()) return@forEach

                val id = idStr.toLong()
                val redisViewCount = redisTemplate.opsForZSet()
                    .score(VIEW_COUNT_KEY, id.toString()) ?: 0.0

                val playlist = playlistRepository.findById(id).orElse(null)
                if (playlist != null) {
                    playlist.viewCount = redisViewCount.toLong()
                    playlistRepository.save(playlist)
                } else {
                    println("Playlist not found in DB for ID: $id")
                }

            } catch (e: NumberFormatException) {
                println("잘못된 Redis 키 형식: $key")
            }
        }
    }


    /**
     * 기존 플레이리스트를 수정합니다.
     *
     * @param id 수정할 플레이리스트 ID
     * @param request 수정할 데이터 DTO
     * @return 수정된 플레이리스트 DTO
     */
    fun updatePlaylist(id: Long, request: PlaylistUpdateDto): PlaylistDto {
        val playlist = playlistRepository.findById(id)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val actor = rq.actor

        request.title?.let { playlist.title = it }
        request.description?.let { playlist.description = it }
        request.isPublic?.let { playlist.isPublic = it }

        return PlaylistDto.fromEntity(playlistRepository.save(playlist), actor)
    }

    /**
     * 플레이리스트를 삭제합니다.
     *
     * @param id 삭제할 플레이리스트 ID
     */
    fun deletePlaylist(id: Long) {
        val playlist = playlistRepository.findById(id)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val actor = rq.actor

        if (playlist.member.getId() != actor.getId()) {
            throw BadRequestException("자신이 소유한 플레이리스트만 삭제할 수 있습니다.")
        }

        if (playlistLikeRepository.existsById_PlaylistId(id)) {
            playlistLikeRepository.deleteById_PlaylistId(id)
        }
        playlistRepository.deleteById(id)
    }


    /**
     * 플레이리스트 좋아요를 토글 처리합니다.
     * Redis로 좋아요 토글, 좋아요 수 업데이트 후 DB에 반영합니다.
     *
     * @param playlistId 대상 플레이리스트 ID
     * @param memberId 좋아요 한 사용자 ID
     */
    @Transactional
    fun likePlaylist(playlistId: Long, memberId: Long) {
        val redisKey = "playlist_like:$playlistId"
        val memberLikedKey = "member_liked_playlists:$memberId"
        val memberStr = memberId.toString()

        val luaScript = """
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                redis.call('SREM', KEYS[1], ARGV[1]); return 0;
            else 
                redis.call('SADD', KEYS[1], ARGV[1]); return 1;
            end
        """.trimIndent()

        val result: Long? = redisTemplate.execute(
            DefaultRedisScript(luaScript, Long::class.java),
            listOf(redisKey),
            memberStr
        )

        if (result != null && result == 1L) {
            redisTemplate.opsForSet().add(memberLikedKey, playlistId.toString())
        } else if (result != null && result == 0L) {
            redisTemplate.opsForSet().remove(memberLikedKey, playlistId.toString())
        }

        val likeCount = redisTemplate.opsForSet().size(redisKey) ?: 0L

        val playlist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        playlist.likeCount = likeCount
        playlistRepository.save(playlist)
    }

    /**
     * 사용자가 특정 플레이리스트에 좋아요를 눌렀는지 여부를 확인합니다.
     *
     * @param playlistId 대상 플레이리스트 ID
     * @param memberId 사용자 ID
     * @return 좋아요 여부
     */
    fun hasLikedPlaylist(playlistId: Long, memberId: Long): Boolean {
        val redisKey = "playlist_like:$playlistId"
        return redisTemplate.opsForSet().isMember(redisKey, memberId.toString()) == true
    }


    /**
     * 특정 플레이리스트의 전체 좋아요 수를 조회합니다.
     *
     * @param playlistId 대상 플레이리스트 ID
     * @return 좋아요 수 (없을 경우 0)
     */
    @Transactional(readOnly = true)
    fun getLikeCount(playlistId: Long): Long {
        val redisKey = "playlist_like:$playlistId"
        val count = redisTemplate.opsForSet().size(redisKey)
        return count ?: 0L
    }

    /**
     * 사용자가 좋아요한 모든 플레이리스트 목록을 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 좋아요한 플레이리스트 DTO 리스트
     */
    @Transactional(readOnly = true)
    fun getLikedPlaylists(memberId: Long): List<PlaylistDto> {
        val likedEntities = playlistLikeRepository.findByIdMemberId(memberId)
        val actor = rq.actor

        val likedPlaylists = likedEntities.map { it.playlist }

        return likedPlaylists.map { PlaylistDto.fromEntity(it, actor) }
    }


    /**
     * Redis에 저장된 사용자의 좋아요 플레이리스트 목록을 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 좋아요한 플레이리스트 DTO 리스트 (없으면 빈 리스트)
     */
    @Transactional(readOnly = true)
    fun getLikedPlaylistsFromRedis(memberId: Long): List<PlaylistDto> {
        val memberLikedKey = "member_liked_playlists:$memberId"
        val playlistIdObjs = redisTemplate.opsForSet().members(memberLikedKey)

        if (playlistIdObjs == null || playlistIdObjs.isEmpty()) {
            return emptyList()
        }

        val playlistIds = playlistIdObjs.map { it.toString().toLong() }
        val playlists = playlistRepository.findAllById(playlistIds)
        val actor = rq.actor

        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }


    /**
     * Redis에 저장된 플레이리스트 좋아요 수를 DB에 동기화합니다.
     *
     * Redis의 좋아요를 기반으로 DB에 PlaylistLike 엔티티로 반영하고,
     * 기존 DB와 비교해 좋아요를 반영합니다.
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    fun syncPlaylistLikesToDB() {
        val keys = redisTemplate.keys("playlist_like:*") ?: return

        for (key in keys) {
            val playlistId = key.split(":").getOrNull(1)?.toLongOrNull() ?: continue

            val playlist = playlistRepository.findById(playlistId).orElse(null) ?: continue
            val rawMemberIds = redisTemplate.opsForSet().members(key) ?: continue
            val memberIds = rawMemberIds.map { it.toString() }.toSet()

            for (memberStr in memberIds) {
                val memberId = memberStr.toLongOrNull() ?: continue
                val member = memberRepository.findById(memberId).orElse(null) ?: continue

                val likeId = PlaylistLike.PlaylistLikeId().apply {
                    this.playlistId = playlistId
                    this.memberId = memberId
                }

                if (!playlistLikeRepository.existsById(likeId)) {
                    val like = PlaylistLike.createLike(playlist, member)
                    playlistLikeRepository.save(like)
                }
            }

            val likeCount = redisTemplate.opsForSet().size(key)
            playlist.likeCount = likeCount ?: 0L
            playlistRepository.save(playlist)

            val currentLikesInDB = playlistLikeRepository.findAllById_PlaylistId(playlistId)
            val currentMemberIdSet = memberIds.mapNotNull { it.toLongOrNull() }.toSet()

            for (dbLike in currentLikesInDB) {
                if (!currentMemberIdSet.contains(dbLike.member.getId())) {
                    playlistLikeRepository.delete(dbLike)
                }
            }
        }
    }

    /**
     * 플레이리스트에 새 아이템을 추가합니다.
     *
     * @param playlistId 아이템을 추가할 플레이리스트 ID
     * @param itemId 추가할 아이템의 ID
     * @param itemType 아이템 타입 (LINK, CURATION)
     * @return 아이템이 추가된 플레이리스트 DTO
     */
    fun addPlaylistItem(
        playlistId: Long,
        itemId: Long,
        itemType: PlaylistItem.PlaylistItemType
    )
            : PlaylistDto {
        val playlist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val newDisplayOrder = playlist.items.size
        val actor = rq.actor

        if (playlist.member.id != actor.id) {
            throw BadRequestException("자신의 플레이리스트에만 아이템을 추가할 수 있습니다.")
        }

        val newItem = PlaylistItem(
            itemId = itemId,
            itemType = itemType,
            playlist = playlist,
            displayOrder = newDisplayOrder
        )

        playlist.items.add(newItem)
        playlistRepository.save(playlist)

        return PlaylistDto.fromEntity(playlist, actor)
    }


    /**
     * 플레이리스트 아이템을 삭제합니다.
     *
     * @param playlistId 아이템을 삭제할 플레이리스트 ID
     * @param itemId 삭제할 아이템의 ID
     */
    @Transactional
    fun deletePlaylistItem(playlistId: Long?, itemId: Long) {
        val playlist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val removed = playlist.items.removeIf { it.id == itemId }
        if (!removed) {
            throw NotFoundException("해당 플레이리스트 아이템을 찾을 수 없습니다.")
        }

        val actor = rq.actor
        if (playlist.member.id != actor.id) {
            throw BadRequestException("자신이 소유한 플레이리스트 아이템만 삭제할 수 있습니다.")
        }

        playlistRepository.save(playlist)
    }


    /**
     * 플레이리스트 아이템의 순서를 변경합니다.
     *
     * @param playlistId 대상 플레이리스트 ID
     * @param orderUpdates 변경할 순서 리스트
     * @return 순서가 변경된 플레이리스트 DTO
     */
    @Transactional
    fun updatePlaylistItemOrder(
        playlistId: Long,
        orderUpdates: List<PlaylistItemOrderUpdateDto>
    ): PlaylistDto {
        val playlist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val totalOrderCount = orderUpdates.sumOf { 1 + (it.children?.size ?: 0) }

        if (playlist.items.size != totalOrderCount) {
            throw BadRequestException(
                "플레이리스트 아이템 개수가 일치하지 않습니다. Expected: ${playlist.items.size}, but received: $totalOrderCount"
            )
        }

        val itemMap = playlist.items.associateBy { it.id }

        var mainIndex = 0
        for (dto in orderUpdates) {
            val mainItem = itemMap[dto.id]
                ?: throw BadRequestException("존재하지 않는 플레이리스트 아이템 ID: ${dto.id}")
            val mainOrder = mainIndex * 100

            mainItem.displayOrder = mainOrder
            mainItem.parentItemId = null

            dto.children?.forEachIndexed { childIndex, childId ->
                val childItem = itemMap[childId]
                    ?: throw BadRequestException("존재하지 않는 그룹 내부 아이템 ID: $childId")

                childItem.displayOrder = mainOrder + childIndex + 1
                childItem.parentItemId = mainItem.id
            }
            mainIndex++
        }

        val actor = rq.actor

        playlistRepository.save(playlist)
        return PlaylistDto.fromEntity(playlist, actor)
    }

    /**
     * 특정 플레이리스트 아이템의 내용을 수정합니다.
     * LinkService를 통해 제목, URL, 설명을 수정합니다.
     *
     * @param playlistId 대상 플레이리스트 ID
     * @param playlistItemId 수정할 아이템 ID
     * @param updateDto 수정할 내용 DTO (제목, URL, 설명)
     * @return 수정된 플레이리스트 DTO
     */
    @Transactional
    fun updatePlaylistItem(
        playlistId: Long,
        playlistItemId: Long,
        updateDto: PlaylistItemUpdateDto
    ): PlaylistDto {
        val playlist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val actor = rq.actor

        val itemToUpdate = playlist.items
            .firstOrNull { it.id == playlistItemId }
            ?: throw NotFoundException("해당 플레이리스트 아이템을 찾을 수 없습니다.")

        if (itemToUpdate.itemType == PlaylistItem.PlaylistItemType.LINK) {
            val updatedLink = linkService.updateLinkDetails(
                requireNotNull(itemToUpdate.link?.id) { "링크 ID가 존재하지 않습니다." },
                updateDto.title,
                updateDto.url,
                updateDto.description
            )
            itemToUpdate.link = updatedLink
        } else {
            throw BadRequestException("현재 아이템은 수정할 수 없습니다.")
        }

        return PlaylistDto.fromEntity(playlist, actor)
    }


    /**
     * 플레이리스트를 추천합니다.
     * Redis에 캐시된 추천 리스트가 있으면 우선 반환하고,
     * 없을 경우 24시간 인기, 전체 인기(조회수 + 좋아요), 동일 태그를 병합해 추천합니다.
     * 추천 결과는 Redis에 30분간 캐싱되며, 정렬 기준(likes, views, combined)에 따라 반환됩니다.
     *
     * @param playlistId 추천 기준이 되는 플레이리스트 ID
     * @param sortType 정렬 기준 (likes, views, combined)
     * @return 추천된 플레이리스트 DTO 리스트
     */
    fun recommendPlaylist(playlistId: Long, sortType: String?): List<PlaylistDto> {
        val cacheKey = "$RECOMMEND_KEY$playlistId"
        val cachedRecommendationsStr = redisTemplate.opsForValue().get(cacheKey) as? String

        if (!cachedRecommendationsStr.isNullOrEmpty()) {
            val cachedRecommendations = cachedRecommendationsStr.split(",").mapNotNull { it.toLongOrNull() }
            println("Redis 캐시 HIT Playlist ID $playlistId | 추천 리스트: $cachedRecommendations")
            return getPlaylistsByIds(cachedRecommendations)
        }

        val trendingRecent = redisTemplate.opsForZSet().reverseRange("trending:24h", 0, 5) ?: emptySet()
        val popularRecent = redisTemplate.opsForZSet().reverseRange("popular:24h", 0, 5) ?: emptySet()

        val trendingPlaylists = redisTemplate.opsForZSet().reverseRange(VIEW_COUNT_KEY, 0, 5) ?: emptySet()
        val popularPlaylists = redisTemplate.opsForZSet().reverseRange(LIKE_COUNT_KEY, 0, 5) ?: emptySet()

        val currentPlaylist = playlistRepository.findById(playlistId).orElse(null)
        val similarPlaylists = currentPlaylist?.let { findSimilarPlaylistsByTag(it) } ?: emptyList()

        val currentMember = rq.actor
        val userPlaylistIds = playlistRepository.findByMember(currentMember).map { it.id }

        val recommendedPlaylistIds = mutableSetOf<Long>()
        addRecommendations(recommendedPlaylistIds, trendingRecent)
        addRecommendations(recommendedPlaylistIds, popularRecent)
        addRecommendations(recommendedPlaylistIds, trendingPlaylists)
        addRecommendations(recommendedPlaylistIds, popularPlaylists)
        similarPlaylists
            .filterNotNull()
            .mapNotNull { it.id }
            .forEach { recommendedPlaylistIds.add(it) }


        recommendedPlaylistIds.removeAll(userPlaylistIds)

        if (recommendedPlaylistIds.isEmpty()) return emptyList()

        redisTemplate.opsForValue().set(
            "$RECOMMEND_KEY$playlistId",
            recommendedPlaylistIds.joinToString(","),
            Duration.ofMinutes(30)
        )

        println("Redis 캐시 저장 완료 Playlist ID $playlistId | 추천 리스트: $recommendedPlaylistIds")


        return getSortedPlaylists(recommendedPlaylistIds.mapNotNull { it }, sortType ?: "combined")

    }

    /**
     * 추천된 플레이리스트 ID 리스트를 받아 PlaylistDto 리스트로 변환합니다.
     *
     * @param playlistIds 추천된 플레이리스트 ID 목록
     * @return 해당 ID의 PlaylistDto 리스트
     */
    private fun getPlaylistsByIds(playlistIds: List<Long>): List<PlaylistDto> {
        val playlists = playlistRepository.findAllById(playlistIds)
        val actor = if (rq.isLogin) rq.actor else null

        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }

    /**
     * 현재 플레이리스트와 동일 태그를 가진 플레이리스트를 찾습니다.
     *
     * 태그가 3개 이상 겹치면 유사 플레이리스트로 판단하여 추천
     * 유사한 플레이리스트가 없을 경우 전체 중 랜덤으로 최대 3개 반환
     *
     * @param currentPlaylist 기준이 되는 플레이리스트
     * @return 유사한 플레이리스트 리스트
     */
    private fun findSimilarPlaylistsByTag(currentPlaylist: Playlist): List<Playlist?> {
        val allPlaylists = playlistRepository.findAll()
        val currentTags = currentPlaylist.tagNames

        val similarPlaylists = allPlaylists.filter { other ->
            other.id != currentPlaylist.id &&
                    other.tagNames.intersect(currentTags).size >= 3
        }

        return if (similarPlaylists.isEmpty()) {
            allPlaylists.shuffled().take(3)
        } else {
            similarPlaylists
        }
    }

    /**
     * Redis에서 가져온 추천 ID를 추천 목록에 병합합니다.
     *
     * @param recommendedPlaylistIds 추천 ID를 담을 Set
     * @param redisResults Redis에서 가져온 추천 ID Object Set
     */
    private fun addRecommendations(
        recommendedPlaylistIds: MutableSet<Long>,
        redisResults: Set<Any>?
    ) {
        redisResults?.forEach { id ->
            id.toString().toLongOrNull()?.let {
                recommendedPlaylistIds.add(it)
            } ?: System.err.println("addRecommendations() 오류: 파싱 불가한 값 = $id")
        }
    }


    /**
     * 정렬 기준에 따라 플레이리스트를 정렬하여 반환합니다.
     * 정렬 기준: 좋아요, 조회수, 좋아요 + 조회수
     *
     * @param playlistIds 정렬할 플레이리스트 ID 리스트
     * @param sortType 정렬 기준 (likes, views, combined)
     * @return 정렬된 PlaylistDto 리스트
     */
    private fun getSortedPlaylists(playlistIds: List<Long>, sortType: String): List<PlaylistDto> {
        val playlists = playlistRepository.findAllById(playlistIds).toMutableList()
        val actor = rq.actor

        when (sortType) {
            "likes" -> playlists.sortByDescending { it.likeCount }
            "views" -> playlists.sortByDescending { it.viewCount }
            "combined" -> playlists.sortByDescending { it.likeCount + it.viewCount }
            else -> {}
        }
        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }

    /**
     * 공개 플레이리스트를 현재 로그인한 사용자의 플레이리스트로 복사합니다.
     *
     * @param playlistId 복사할 공개 플레이리스트 ID
     * @return 복사된 내 플레이리스트 DTO
     */
    fun addPublicPlaylist(playlistId: Long): PlaylistDto {
        val publicPlaylist = playlistRepository.findById(playlistId)
            .orElseThrow { NotFoundException("해당 플레이리스트를 찾을 수 없습니다.") }

        val actor = rq.actor

        val copiedPlaylist = Playlist(
            title = publicPlaylist.title,
            description = publicPlaylist.description,
            isPublic = false,
            member = actor
        )

        val savedPlaylist = playlistRepository.save(copiedPlaylist)

        publicPlaylist.items.forEach { item ->
            val copiedItem = PlaylistItem(
                itemId = item.itemId,
                itemType = item.itemType,
                displayOrder = item.displayOrder,
                playlist = savedPlaylist
            )
            savedPlaylist.items.add(copiedItem)
        }

        return PlaylistDto.fromEntity(savedPlaylist, actor)
    }

    /**
     * 사용자가 작성한 플레이리스트 중 특정 큐레이션을 포함한 플레이리스를 조회합니다.
     * 특정 큐레이션에 연결된 사용자의 플레이리스트 목록을 조회할 때 사용됩니다.
     *
     * @param member 조회할 사용자
     * @param curationId 포함된 큐레이션 ID
     * @return 해당 조건을 만족하는 플레이리스트 DTO 리스트
     */
    fun getPlaylistsByMemberAndCuration(member: Member, curationId: Long): List<PlaylistDto> {
        val playlists = playlistRepository.findByMemberAndCuration(member, curationId)
        val actor = rq.actor

        return playlists.map { PlaylistDto.fromEntity(it, actor) }
    }
}