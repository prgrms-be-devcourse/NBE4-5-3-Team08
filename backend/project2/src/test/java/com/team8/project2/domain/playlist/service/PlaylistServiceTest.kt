package com.team8.project2.domain.playlist.service

import com.team8.project2.domain.curation.tag.repository.TagRepository
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.playlist.dto.PlaylistCreateDto
import com.team8.project2.domain.playlist.dto.PlaylistItemOrderUpdateDto
import com.team8.project2.domain.playlist.dto.PlaylistItemUpdateDto
import com.team8.project2.domain.playlist.dto.PlaylistUpdateDto
import com.team8.project2.domain.playlist.entity.Playlist
import com.team8.project2.domain.playlist.entity.PlaylistItem
import com.team8.project2.domain.playlist.repository.PlaylistItemRepository
import com.team8.project2.domain.playlist.repository.PlaylistLikeRepository
import com.team8.project2.domain.playlist.repository.PlaylistRepository
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.BadRequestException
import com.team8.project2.global.exception.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaylistServiceTest {

    private lateinit var playlistService: PlaylistService

    @Mock
    private lateinit var playlistRepository: PlaylistRepository

    @Mock
    lateinit var playlistItemRepository: PlaylistItemRepository

    @Mock
    lateinit var playlistLikeRepository: PlaylistLikeRepository

    @Mock
    lateinit var tagRepository: TagRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var linkService: LinkService

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var zSetOperations: ZSetOperations<String, Any>

    @Mock
    private lateinit var setOperations: SetOperations<String, Any>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>

    @Mock
    private lateinit var rq: Rq

    private lateinit var samplePlaylist: Playlist
    private lateinit var sampleMember: Member

    @BeforeEach
    fun setUp() {
        sampleMember = Member.builder()
            .id(1L)
            .memberId("test123")
            .username("테스트 유저")
            .password("testPassword123!")
            .role(RoleEnum.MEMBER)
            .email("test@example.com")
            .profileImage(null)
            .introduce("자기소개 테스트")
            .build()

        samplePlaylist = Playlist(
            id = 1L,
            title = "테스트 플레이리스트",
            description = "테스트 설명",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            items = mutableListOf(),
            tags = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )

        whenever(rq.actor).thenReturn(sampleMember)

        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)

        playlistService = PlaylistService(
            playlistRepository,
            redisTemplate,
            memberRepository,
            playlistLikeRepository,
            rq,
            linkService
        )

    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 생성해야 한다.")
    fun shouldCreatePlaylistSuccessfully() {
        // Given
        val request = PlaylistCreateDto(
            title = "새 플레이리스트",
            description = "새로운 설명"
        )

        val newPlaylist = Playlist(
            id = 2L,
            title = request.title,
            description = request.description,
            tags = mutableSetOf(),
            member = sampleMember,
            items = mutableListOf(),
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now()
        )


        whenever(playlistRepository.save(any())).thenReturn(newPlaylist)

        // When
        val createdPlaylist = playlistService.createPlaylist(request)

        // Then
        assertNotNull(createdPlaylist)
        assertEquals(request.title, createdPlaylist.title)
        assertEquals(request.description, createdPlaylist.description)
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 조회해야 한다.")
    fun shouldRetrievePlaylistSuccessfully() {
        // Given
        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))

        // When
        val foundPlaylist = playlistService.getPlaylist(1L, MockHttpServletRequest())

        // Then
        assertNotNull(foundPlaylist)
        assertEquals(samplePlaylist.title, foundPlaylist.title)
        assertEquals(samplePlaylist.description, foundPlaylist.description)
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트 조회 시 NotFoundException이 발생해야 한다.")
    fun shouldThrowNotFoundExceptionWhenPlaylistDoesNotExist() {
        // Given
        whenever(playlistRepository.findById(99L)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NotFoundException::class.java) {
            playlistService.getPlaylist(99L, MockHttpServletRequest())
        }
    }

    @Test
    @DisplayName("현재 로그인한 사용자의 모든 플레이리스트를 정상적으로 조회해야 한다.")
    fun shouldRetrieveAllPlaylistsSuccessfully() {
        // Given
        val playlists = listOf(samplePlaylist)
        whenever(playlistRepository.findByMember(sampleMember)).thenReturn(playlists)

        // When
        val foundPlaylists = playlistService.getAllPlaylists()

        // Then
        assertFalse(foundPlaylists.isEmpty())
        assertEquals(1, foundPlaylists.size)
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 수정해야 한다.")
    fun shouldUpdatePlaylistSuccessfully() {
        // Given
        val request = PlaylistUpdateDto(
            title = "수정된 플레이리스트",
            description = "수정된 설명"
        )

        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))
        whenever(playlistRepository.save(any())).thenReturn(samplePlaylist)

        // When
        val updatedPlaylist = playlistService.updatePlaylist(1L, request)

        // Then
        assertNotNull(updatedPlaylist)
        assertEquals(request.title, updatedPlaylist.title)
        assertEquals(request.description, updatedPlaylist.description)
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 삭제해야 한다.")
    fun shouldDeletePlaylistSuccessfully() {
        // Given
        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))
        doNothing().whenever(playlistRepository).deleteById(1L)

        // When & Then
        assertDoesNotThrow { playlistService.deletePlaylist(1L) }
        verify(playlistRepository, times(1)).deleteById(1L)
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트 삭제 시 NotFoundException이 발생해야 한다.")
    fun shouldThrowNotFoundExceptionWhenDeletingNonExistingPlaylist() {
        // Given
        whenever(playlistRepository.findById(99L)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NotFoundException::class.java) { playlistService.deletePlaylist(99L) }
    }

    @Test
    @DisplayName("플레이리스트에 아이템을 추가할 수 있다.")
    fun addPlaylistItem() {
        // Given
        val newItemId = 100L

        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))
        whenever(playlistRepository.save(any())).thenAnswer { it.arguments[0] }

        // When
        val updatedPlaylist = playlistService.addPlaylistItem(1L, newItemId, PlaylistItem.PlaylistItemType.LINK)

        // Then
        assertNotNull(updatedPlaylist)
        assertEquals("테스트 플레이리스트", updatedPlaylist.title)
        assertFalse(updatedPlaylist.items.isEmpty())
        assertEquals(newItemId, updatedPlaylist.items[0].itemId)
        assertEquals("LINK", updatedPlaylist.items[0].itemType)
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 플레이리스트에 아이템을 추가할 수 없다.")
    fun addPlaylistItemNotFound() {
        // Given
        val newItemId = 100L
        whenever(playlistRepository.findById(1L)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NotFoundException::class.java) {
            playlistService.addPlaylistItem(1L, newItemId, PlaylistItem.PlaylistItemType.LINK)
        }
    }

    @Test
    @DisplayName("플레이리스트에서 아이템을 삭제할 수 있다.")
    fun deletePlaylistItem() {
        // Given
        val itemDbIdToDelete = 1L

        val item1 = PlaylistItem(
            id = 1L,
            itemId = 100L,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            displayOrder = 0,
            playlist = samplePlaylist
        )

        val item2 = PlaylistItem(
            id = 2L,
            itemId = 101L,
            itemType = PlaylistItem.PlaylistItemType.CURATION,
            displayOrder = 1,
            playlist = samplePlaylist
        )

        samplePlaylist.items = mutableListOf(item1, item2)
        whenever(playlistRepository.findById(samplePlaylist.id)).thenReturn(Optional.of(samplePlaylist))

        // When
        playlistService.deletePlaylistItem(samplePlaylist.id, itemDbIdToDelete)

        // Then
        assertFalse(samplePlaylist.items.any { it.id == itemDbIdToDelete })
        verify(playlistRepository, times(1)).save(samplePlaylist)
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 아이템은 삭제할 수 없다.")
    fun deletePlaylistItemNotFound() {
        // Given
        val itemIdToDelete = 100L
        samplePlaylist.items = mutableListOf()

        whenever(playlistRepository.findById(samplePlaylist.id))
            .thenReturn(Optional.of(samplePlaylist))

        // When & Then
        assertThrows(NotFoundException::class.java) {
            playlistService.deletePlaylistItem(samplePlaylist.id, itemIdToDelete)
        }
    }

    @Test
    @DisplayName("플레이리스트 아이템 순서를 변경할 수 있다.")
    fun updatePlaylistItemOrder() {
        // Given
        val item1 = PlaylistItem(
            id = 1L,
            itemId = 100L,
            displayOrder = 0,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            playlist = samplePlaylist,
            curation = null,
            link = null
        )
        val item2 = PlaylistItem(
            id = 2L,
            itemId = 101L,
            displayOrder = 1,
            itemType = PlaylistItem.PlaylistItemType.CURATION,
            playlist = samplePlaylist,
            curation = null,
            link = null
        )

        val item3 = PlaylistItem(
            id = 3L,
            itemId = 102L,
            displayOrder = 2,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            playlist = samplePlaylist,
            curation = null,
            link = null
        )
        samplePlaylist.items = mutableListOf(item1, item2, item3)

        val newOrder = listOf(
            PlaylistItemOrderUpdateDto(id = 3L, children = emptyList()),
            PlaylistItemOrderUpdateDto(id = 1L, children = emptyList()),
            PlaylistItemOrderUpdateDto(id = 2L, children = emptyList())
        )

        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))
        whenever(playlistRepository.save(any())).thenReturn(samplePlaylist)

        // When
        val updatedDto = playlistService.updatePlaylistItemOrder(1L, newOrder)

        // Then
        assertEquals(0, samplePlaylist.items.first { it.id == 3L }.displayOrder)
        assertEquals(100, samplePlaylist.items.first { it.id == 1L }.displayOrder)
        assertEquals(200, samplePlaylist.items.first { it.id == 2L }.displayOrder)

        assertNotNull(updatedDto)
        assertEquals("테스트 플레이리스트", updatedDto.title)
    }

    @Test
    @DisplayName("실패 - 플레이리스트 아이템 순서 변경 시 아이템 개수가 일치해야 한다.")
    fun updatePlaylistItemOrder_itemCount() {
        // Given
        val item1 = PlaylistItem(
            id = 1L,
            itemId = 100L,
            displayOrder = 0,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            playlist = samplePlaylist,
            link = null
        )

        val item2 = PlaylistItem(
            id = 2L,
            itemId = 101L,
            displayOrder = 1,
            itemType = PlaylistItem.PlaylistItemType.CURATION,
            playlist = samplePlaylist,
            link = null
        )

        val item3 = PlaylistItem(
            id = 3L,
            itemId = 102L,
            displayOrder = 2,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            playlist = samplePlaylist,
            link = null
        )

        samplePlaylist.items = mutableListOf(item1, item2, item3)

        val newOrder = listOf(
            PlaylistItemOrderUpdateDto(id = 1L, children = emptyList()),
            PlaylistItemOrderUpdateDto(id = 2L, children = emptyList())

        )

        whenever(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist))

        // When & Then
        assertThrows(BadRequestException::class.java) {
            playlistService.updatePlaylistItemOrder(1L, newOrder)
        }
    }

    @Test
    @DisplayName("좋아요가 Redis에서 정상적으로 증가해야 한다.")
    fun shouldIncreaseLikeCountInRedis() {
        val playlistId = 1L
        val memberId = 1L

        // Given
        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist))
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(
            redisTemplate.execute(
                any<DefaultRedisScript<Long>>(),
                eq(listOf("playlist_like:$playlistId")),
                eq(memberId.toString())
            )
        )
            .thenReturn(1L)
        whenever(setOperations.size("playlist_like:$playlistId")).thenReturn(1L)

        // When
        playlistService.likePlaylist(playlistId, memberId)

        // Then
        verify(redisTemplate, times(1)).execute(
            any<DefaultRedisScript<Long>>(),
            eq(listOf("playlist_like:$playlistId")),
            eq(memberId.toString())
        )
        assertEquals(1L, samplePlaylist.likeCount)
    }

    @Test
    @DisplayName("추천 API가 Redis 캐싱을 사용하여 정상적으로 동작해야 한다.")
    fun shouldRetrieveRecommendedPlaylistsFromCache() {
        val playlistId = 1L
        val cachedRecommendationsStr = "2,3"

        // Given - Redis에서 추천 데이터가 존재하는 경우
        whenever(valueOperations.get("playlist:recommend:$playlistId")).thenReturn(cachedRecommendationsStr)
        whenever(playlistRepository.findAllById(listOf(2L, 3L)))
            .thenReturn(
                listOf(
                    Playlist(
                        id = 2L,
                        title = "추천1",
                        description = "설명1",
                        tags = mutableSetOf(),
                        member = sampleMember,
                        createdAt = LocalDateTime.now()
                    ),
                    Playlist(
                        id = 3L,
                        title = "추천2",
                        description = "설명2",
                        tags = mutableSetOf(),
                        member = sampleMember,
                        createdAt = LocalDateTime.now()
                    )
                )
            )

        // When
        val recommendations = playlistService.recommendPlaylist(playlistId, "likes")

        // Then
        assertEquals(2, recommendations.size)
        verify(valueOperations, times(1)).get("playlist:recommend:$playlistId")
        verify(playlistRepository, times(1)).findAllById(listOf(2L, 3L))
    }

    @Test
    @DisplayName("추천 플레이리스트가 좋아요 순으로 정렬되어야 한다.")
    fun shouldSortRecommendedPlaylistsByLikes() {
        // Given
        val playlistId = 1L
        val sortType = "likes"

        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist))

        // 추천 대상 플레이리스트 모킹 (likeCount 부여)
        val p2 = Playlist(
            id = 2L,
            title = "추천 플레이리스트1",
            description = "설명1",
            tags = mutableSetOf(),
            likeCount = 10L,
            member = sampleMember,
            createdAt = LocalDateTime.now()
        )
        val p3 = Playlist(
            id = 3L,
            title = "추천 플레이리스트2",
            description = "설명2",
            tags = mutableSetOf(),
            likeCount = 5L,
            member = sampleMember,
            createdAt = LocalDateTime.now()
        )

        val mockPlaylists = listOf(p2, p3)
        whenever(playlistRepository.findAllById(any())).thenReturn(mockPlaylists)

        whenever(valueOperations.get("playlist:recommend:$playlistId")).thenReturn(null)

        // Redis 정렬된 집합 호출
        whenever(zSetOperations.reverseRange(eq("playlist:like_count:"), any(), any()))
            .thenReturn(setOf("2", "3"))

        // Redis 나머지 빈 집합 설정
        whenever(zSetOperations.reverseRange(eq("playlist:view_count:"), any(), any()))
            .thenReturn(emptySet())
        whenever(zSetOperations.reverseRange(eq("trending:24h"), any(), any()))
            .thenReturn(emptySet())
        whenever(zSetOperations.reverseRange(eq("popular:24h"), any(), any()))
            .thenReturn(emptySet())

        // 사용자의 플레이리스트 없음
        whenever(playlistRepository.findByMember(sampleMember)).thenReturn(emptyList())

        // When
        val result = playlistService.recommendPlaylist(playlistId, sortType)

        // Then
        assertEquals(2, result.size)
        assertEquals(2L, result[0].id)
        assertEquals("추천 플레이리스트1", result[0].title)
        assertEquals(3L, result[1].id)
        assertEquals("추천 플레이리스트2", result[1].title)

        verify(playlistRepository, times(1)).findById(playlistId)
    }

    @Test
    @DisplayName("추천 플레이리스트가 조회수 순으로 정렬되어야 한다.")
    fun shouldSortRecommendedPlaylistsByViews() {
        // Given
        val playlistId = 1L
        val sortType = "views"

        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist))

        val p2 = Playlist(
            id = 2L,
            title = "추천1",
            description = "설명1",
            tags = mutableSetOf(),
            viewCount = 200L,
            member = sampleMember,
            createdAt = LocalDateTime.now(),
        )
        val p3 = Playlist(
            id = 3L,
            title = "추천2",
            description = "설명2",
            tags = mutableSetOf(),
            viewCount = 100L,
            member = sampleMember,
            createdAt = LocalDateTime.now(),
        )

        val mockPlaylists = listOf(p2, p3)
        whenever(playlistRepository.findAllById(any())).thenReturn(mockPlaylists)
        whenever(playlistRepository.findAll()).thenReturn(mockPlaylists)

        whenever(valueOperations.get("playlist:recommend:$playlistId")).thenReturn(null)

        // Redis ZSet 모킹: 빈 집합
        whenever(zSetOperations.reverseRange(eq("playlist:view_count:"), any(), any()))
            .thenReturn(emptySet())
        whenever(zSetOperations.reverseRange(eq("playlist:like_count:"), any(), any()))
            .thenReturn(emptySet())
        whenever(zSetOperations.reverseRange(eq("trending:24h"), any(), any()))
            .thenReturn(emptySet())
        whenever(zSetOperations.reverseRange(eq("popular:24h"), any(), any()))
            .thenReturn(emptySet())

        // 사용자의 플레이리스트 없음
        whenever(playlistRepository.findByMember(sampleMember)).thenReturn(emptyList())
        whenever(rq.actor).thenReturn(sampleMember)

        // When
        val recommendations = playlistService.recommendPlaylist(playlistId, sortType)

        // Then
        assertNotNull(recommendations)
        assertEquals(2, recommendations.size)
        assertEquals(2L, recommendations[0].id)
        assertEquals("추천1", recommendations[0].title)
        assertEquals(3L, recommendations[1].id)
        assertEquals("추천2", recommendations[1].title)

        verify(playlistRepository, times(1)).findById(playlistId)
    }

    @Test
    @DisplayName("추천 플레이리스트가 좋아요+조회수 복합 점수 순으로 정렬되어야 한다.")
    fun shouldSortRecommendedPlaylistsByCombined() {
        val playlistId = 1L
        val sortType = "combined"

        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist))

        val p2 = Playlist(
            id = 2L,
            title = "추천1",
            description = "설명1",
            tags = mutableSetOf(),
            viewCount = 150L,
            likeCount = 20L,
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )
        val p3 = Playlist(
            id = 3L,
            title = "추천2",
            description = "설명2",
            tags = mutableSetOf(),
            viewCount = 100L,
            likeCount = 30L,
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )

        val playlists = listOf(p2, p3)

        // findAllById()와 findAll()이 추천 대상 리스트를 반환하도록 설정
        whenever(playlistRepository.findAllById(any())).thenReturn(playlists)
        whenever(playlistRepository.findAll()).thenReturn(playlists)

        whenever(valueOperations.get("playlist:recommend:$playlistId")).thenReturn(null)
        whenever(zSetOperations.reverseRange(any(), any(), any()))
            .thenReturn(emptySet())

        whenever(rq.actor).thenReturn(sampleMember)

        // When
        val recommendations = playlistService.recommendPlaylist(playlistId, sortType)

        // Then
        assertNotNull(recommendations)
        assertEquals(2, recommendations.size)
        assertEquals(2L, recommendations[0].id)
        assertEquals("추천1", recommendations[0].title)
        assertEquals(3L, recommendations[1].id)
        assertEquals("추천2", recommendations[1].title)

        verify(playlistRepository, times(1)).findById(playlistId)
    }

    @Test
    @DisplayName("Redis 캐싱이 없을 때 추천 알고리즘을 실행해야 한다.")
    fun shouldRunRecommendationAlgorithmIfCacheMiss() {
        // Given
        val playlistId = 1L
        val sortType = "combined"

        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)

        val sampleMember = Member.builder()
            .id(1L)
            .memberId("test123")
            .username("테스트 유저")
            .password("1234")
            .role(RoleEnum.MEMBER)
            .email("test@email.com")
            .profileImage(null)
            .introduce("테스123.")
            .build()

        val samplePlaylist = Playlist(
            id = playlistId,
            title = "테스트 플레이리스트",
            description = "테스트 설명",
            tags = mutableSetOf(),
            member = sampleMember,
            createdAt = LocalDateTime.now()
        )

        whenever(valueOperations.get("playlist:recommend:$playlistId")).thenReturn(null)

        val trendingPlaylists = setOf("2", "3")
        val popularPlaylists = setOf("3", "4")

        whenever(zSetOperations.reverseRange(eq("playlist:view_count:"), eq(0L), eq(5L)))
            .thenReturn(trendingPlaylists)
        whenever(zSetOperations.reverseRange(eq("playlist:like_count:"), eq(0L), eq(5L)))
            .thenReturn(popularPlaylists)

        whenever(zSetOperations.reverseRange(eq("trending:24h"), eq(0L), eq(5L)))
            .thenReturn(trendingPlaylists)
        whenever(zSetOperations.reverseRange(eq("popular:24h"), eq(0L), eq(5L)))
            .thenReturn(popularPlaylists)

        val recommendedPlaylistIds = listOf(2L, 3L, 4L)
        val mockPlaylists = listOf(
            Playlist(
                id = 2L,
                title = "추천1",
                description = "설명1",
                tags = mutableSetOf(),
                member = sampleMember,
                createdAt = LocalDateTime.now()
            ),
            Playlist(
                id = 3L,
                title = "추천2",
                description = "설명2",
                tags = mutableSetOf(),
                member = sampleMember,
                createdAt = LocalDateTime.now()
            ),
            Playlist(
                id = 4L,
                title = "추천3",
                description = "설명3",
                tags = mutableSetOf(),
                member = sampleMember,
                createdAt = LocalDateTime.now()
            )
        )

        whenever(playlistRepository.findAllById(any())).thenReturn(mockPlaylists)
        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist))
        whenever(rq.actor).thenReturn(sampleMember)

        // When
        val recommendations = playlistService.recommendPlaylist(playlistId, sortType)

        // Then
        assertEquals(3, recommendations.size)
        verify(valueOperations, times(1))
            .set(eq("playlist:recommend:$playlistId"), any<String>(), any<Duration>())
    }

    @Test
    @DisplayName("공개 플레이리스트를 내 플레이리스트로 복사한다.")
    fun addPublicPlaylistToMyPlaylist() {
        // given
        val member = Member.builder()
            .id(1L)
            .memberId("member123")
            .username("testUser")
            .password("1234")
            .role(RoleEnum.MEMBER)
            .email("test@email.com")
            .profileImage(null)
            .introduce("test")
            .build()

        val originalOwner = Member.builder()
            .id(2L)
            .memberId("owner123")
            .username("originalOwner")
            .password("ownerPw")
            .role(RoleEnum.MEMBER)
            .email("owner@email.com")
            .profileImage(null)
            .introduce("원본 유저")
            .build()

        val originalPlaylist = Playlist(
            id = 100L,
            title = "공개 플레이리스트",
            description = "공개 설명",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            tags = mutableSetOf(),
            items = mutableListOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = originalOwner
        )

        whenever(rq.actor).thenReturn(member)
        whenever(playlistRepository.findById(originalPlaylist.id))
            .thenReturn(Optional.of(originalPlaylist))
        whenever(playlistRepository.save(any()))
            .thenAnswer { invocation ->
                val playlist = invocation.arguments[0] as Playlist
                Playlist(
                    id = 101L,
                    title = playlist.title,
                    description = playlist.description,
                    isPublic = playlist.isPublic,
                    viewCount = playlist.viewCount,
                    likeCount = playlist.likeCount,
                    tags = playlist.tags,
                    items = playlist.items,
                    createdAt = LocalDateTime.now(),
                    modifiedAt = playlist.modifiedAt,
                    member = playlist.member
                )
            }


        // when
        val result = playlistService.addPublicPlaylist(requireNotNull(originalPlaylist.id))

        // then
        assertNotNull(result)
        assertTrue(result.isOwner)
        assertEquals(originalPlaylist.title, result.title)
        assertEquals(originalPlaylist.description, result.description)
        assertFalse(result.isPublic)
        assertEquals(originalPlaylist.items.size, result.items.size)
    }

    @Test
    @DisplayName("플레이리스트 아이템의 내용을 수정한다")
    fun updatePlaylistLinkItemContent() {
        // given
        val playlistId = 1L
        val playlistItemId = 10L
        val linkId = 100L

        val member = Member.builder()
            .id(1L)
            .memberId("member123")
            .username("testUser")
            .password("1234")
            .email("test@email.com")
            .role(RoleEnum.MEMBER)
            .profileImage(null)
            .introduce("test")
            .build()


        val link = Link.builder()
            .id(linkId)
            .url("https://old-url.com")
            .title("기존 제목")
            .description("기존 설명")
            .build()

        val playlist = Playlist(
            id = playlistId,
            title = "수정 가능한 플리",
            description = "설명",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            tags = mutableSetOf(),
            items = mutableListOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = member
        )

        val playlistItem = PlaylistItem(
            id = playlistItemId,
            itemId = linkId,
            itemType = PlaylistItem.PlaylistItemType.LINK,
            displayOrder = 0,
            playlist = playlist,
            link = link,
            curation = null
        )

        playlist.items.add(playlistItem)

        val updateDto = PlaylistItemUpdateDto(
            title = "수정된 링크 제목",
            description = "수정된 링크 설명",
            url = "https://new-url.com"
        )

        whenever(rq.actor).thenReturn(member)
        whenever(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist))
        lenient().whenever(playlistRepository.save(any<Playlist>())).thenAnswer { invocation: InvocationOnMock ->
            invocation.getArgument<Playlist>(0)
        }

        whenever(
            linkService.updateLinkDetails(
                linkId = eq(linkId),
                title = eq(updateDto.title),
                url = eq(updateDto.url),
                description = eq(updateDto.description)
            )
        ).thenReturn(
            Link(
                id = linkId,
                url = updateDto.url,
                title = updateDto.title,
                description = updateDto.description,
                createdAt = LocalDateTime.now()
            )
        )

        // when
        val result = playlistService.updatePlaylistItem(playlistId, playlistItemId, updateDto)

        // then
        assertNotNull(result)
        assertEquals(1, result.items.size)
        val updatedItem = result.items[0]
        assertEquals("LINK", updatedItem.itemType)
        assertEquals("https://new-url.com", updatedItem.url)
        assertEquals("수정된 링크 제목", updatedItem.title)
        assertEquals("수정된 링크 설명", updatedItem.description)
    }

    @Test
    @DisplayName("현재 로그인한 사용자의 좋아요 여부를 확인한다")
    fun checkUserLikedPlaylist() {
        // given
        val playlistId = 1L
        val memberId = 1L
        val redisKey = "playlist_like:$playlistId"

        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(setOperations.isMember(redisKey, memberId.toString())).thenReturn(true)

        // when
        val isLiked = playlistService.hasLikedPlaylist(playlistId, memberId)

        // then
        assertTrue(isLiked)
        verify(setOperations, times(1)).isMember(redisKey, memberId.toString())
    }

    @Test
    @DisplayName("플레이리스트의 좋아요 개수를 조회한다")
    fun getPlaylistLikeCount() {
        // given
        val playlistId = 1L
        val redisKey = "playlist_like:$playlistId"

        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(setOperations.size(redisKey)).thenReturn(42L)

        // when
        val likeCount = playlistService.getLikeCount(playlistId)

        // then
        assertEquals(42L, likeCount)
        verify(setOperations, times(1)).size(redisKey)
    }

    @Test
    @DisplayName("현재 로그인한 사용자가 좋아요한 플레이리스트 목록 조회한다")
    fun getLikedPlaylists() {
        // given
        val memberId = 1L
        val redisKey = "member_liked_playlists:$memberId"

        val playlist1 = Playlist(
            id = 10L,
            title = "좋아요한 첫 번째 플레이리스트",
            description = "설명1",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            items = mutableListOf(),
            tags = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )

        val playlist2 = Playlist(
            id = 20L,
            title = "좋아요한 두 번째 플레이리스트",
            description = "설명2",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            items = mutableListOf(),
            tags = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )


        val likedIds = setOf("10", "20")

        whenever(rq.actor).thenReturn(sampleMember)
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(setOperations.members(redisKey)).thenReturn(likedIds)
        whenever(playlistRepository.findAllById(any())).thenReturn(listOf(playlist1, playlist2))

        // when
        val result = playlistService.getLikedPlaylistsFromRedis(memberId)

        // then
        assertEquals(2, result.size)
        val ids = result.map { it.id }
        assertTrue(ids.containsAll(listOf(10L, 20L)))

        verify(setOperations, times(1)).members(redisKey)
        verify(playlistRepository, times(1)).findAllById(any())
    }

    @Test
    @DisplayName("공개된 전체 플레이리스트를 조회한다")
    fun getAllPublicPlaylists() {
        // given
        val actor = sampleMember
        val playlist1 = Playlist(
            id = 10L,
            title = "좋아요한 첫 번째 플레이리스트",
            description = "설명1",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            items = mutableListOf(),
            tags = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )

        val playlist2 = Playlist(
            id = 20L,
            title = "좋아요한 두 번째 플레이리스트",
            description = "설명2",
            isPublic = true,
            viewCount = 0L,
            likeCount = 0L,
            items = mutableListOf(),
            tags = mutableSetOf(),
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            member = sampleMember
        )


        whenever(rq.isLogin).thenReturn(true)
        whenever(rq.actor).thenReturn(actor)
        whenever(playlistRepository.findAllByIsPublicTrue())
            .thenReturn(listOf(playlist1, playlist2))

        // when
        val result = playlistService.getAllPublicPlaylists()

        // then
        assertEquals(2, result.size)
        assertEquals(10L, result[0].id)
        assertEquals(20L, result[1].id)

        verify(playlistRepository, times(1)).findAllByIsPublicTrue()
    }
}