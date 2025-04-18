package com.team8.project2.domain.playlist.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.playlist.dto.*
import com.team8.project2.domain.playlist.entity.PlaylistItem
import com.team8.project2.domain.playlist.entity.PlaylistItem.PlaylistItemType
import com.team8.project2.domain.playlist.service.PlaylistService
import com.team8.project2.global.Rq
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class ApiV1PlaylistControllerTest {
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @InjectMocks
    private lateinit var playlistController: ApiV1PlaylistController

    @Mock
    private lateinit var playlistService: PlaylistService

    @Mock
    private lateinit var rq: Rq

    @Mock
    private lateinit var linkService: LinkService

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(playlistController).build()
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 생성해야 한다.")
    @Throws(Exception::class)
    fun shouldCreatePlaylistSuccessfully() {
        // Given
        val request = PlaylistCreateDto(
            title = "New Playlist", description = "Description", isPublic = true
        )

        val response = PlaylistDto(
            id = 1L,
            title = request.title,
            description = request.description,
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = emptyList(),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(playlistService.createPlaylist(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/playlists").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("New Playlist"))
    }

    @DisplayName("플레이리스트에 링크 추가가 정상적으로 이루어져야 한다.")
    @Test
    @Throws(Exception::class)
    fun addLinkToPlaylist() {
        // Given
        val playlistId = 1L
        val linkId = 100L

        val linkReqDTO = LinkReqDTO(
            url = "https://example.com",
            title = "테스트 링크",
            description = "링크 설명"
        )

        val dummyLink = Link.builder()
            .id(linkId)
            .title(linkReqDTO.title.orEmpty())
            .url(linkReqDTO.url.orEmpty())
            .description(linkReqDTO.description.orEmpty())
            .build()


        val playlistItem = PlaylistItemDto(
            id = null,
            itemId = linkId,
            itemType = "LINK",
            title = dummyLink.title.orEmpty(),
            description = dummyLink.description.orEmpty(),
            url = dummyLink.url.orEmpty(),
            curationId = null,
            parentItemId = null
        )



        val playlistDto = PlaylistDto(
            id = playlistId,
            title = "테스트 플레이리스트",
            description = "설명",
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = listOf(playlistItem),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(linkService.addLink(any())).thenReturn(dummyLink)
        whenever(playlistService.addPlaylistItem(eq(playlistId), eq(linkId), eq(PlaylistItemType.LINK))).thenReturn(
            playlistDto
        )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/playlists/{id}/items/link", playlistId)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(linkReqDTO))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("테스트 플레이리스트"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].itemType").value("LINK"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].itemId").value(linkId))
    }


    @Test
    @DisplayName("플레이리스트에서 아이템이 삭제되어야 한다.")
    @Throws(Exception::class)
    fun deletePlaylistItem() {
        val playlistId = 1L
        val itemId = 100L

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/playlists/{id}/items/{itemId}", playlistId, itemId)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 아이템이 삭제되었습니다."))

        verify(playlistService, times(1)).deletePlaylistItem(playlistId, itemId)
    }


    @Test
    @DisplayName("플레이리스트에서 아이템 순서가 변경되어야 한다.")
    @Throws(Exception::class)
    fun updatePlaylistItemOrder() {
        // Given
        val updatedDto = PlaylistDto(
            id = 1L,
            title = "테스트 플레이리스트",
            description = "테스트 설명",
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = emptyList(),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        val jsonContent = """[{"id":3},{"id":1},{"id":2}]"""

        whenever(playlistService.updatePlaylistItemOrder(eq(1L), any())).thenReturn(updatedDto)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/playlists/1/items/order").contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 아이템 순서가 변경되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("테스트 플레이리스트"))
    }

    @Test
    @DisplayName("좋아요 증가 API가 정상적으로 호출되어야 한다.")
    fun shouldIncreaseLikeCount() {
        // Given
        val playlistId = 1L
        val loginId = 100L

        val mockMember = Member(
            id = 100L,
            username = "testuser",
            password = "testpass",
            role = RoleEnum.MEMBER
        )

        whenever(rq.actor).thenReturn(mockMember)


        whenever(rq.actor).thenReturn(mockMember)
        doNothing().whenever(playlistService).likePlaylist(playlistId, loginId)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/playlists/{id}/like", playlistId)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요 상태가 토글되었습니다."))

        verify(playlistService, times(1)).likePlaylist(playlistId, loginId)
    }

    @Test
    @DisplayName("플레이리스트의 추천 목록을 정렬하여 조회할 수 있다.")
    fun getRecommendedPlaylistsWithSorting() {
        val playlistId = 1L

        // 정렬 기준
        val sortType1 = "views"
        val sortType2 = "likes"
        val sortType3 = "combined"

        val recommended = listOf(
            PlaylistDto(
                id = 2L,
                title = "추천 플레이리스트1",
                description = "설명1",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            ),
            PlaylistDto(
                id = 3L,
                title = "추천 플레이리스트2",
                description = "설명2",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            )
        )

        whenever(playlistService.recommendPlaylist(playlistId, sortType1)).thenReturn(recommended)
        whenever(playlistService.recommendPlaylist(playlistId, sortType2)).thenReturn(recommended)
        whenever(playlistService.recommendPlaylist(playlistId, sortType3)).thenReturn(recommended)

        // views 정렬 기준
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/playlists/{id}/recommendation", playlistId)
                .param("sortType", sortType1)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("추천 플레이리스트1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].title").value("추천 플레이리스트2"))

        // likes 정렬 기준
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/playlists/{id}/recommendation", playlistId)
                .param("sortType", sortType2)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("추천 플레이리스트1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].title").value("추천 플레이리스트2"))

        // combined 정렬 기준
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/playlists/{id}/recommendation", playlistId)
                .param("sortType", sortType3)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("추천 플레이리스트1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].title").value("추천 플레이리스트2"))

        // 호출 검증
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType1)
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType2)
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType3)
    }


    @Test
    @DisplayName("플레이리스트 추천 기능이 정렬 기준에 따라 정상 동작해야 한다.")
    fun shouldReturnRecommendedPlaylistsSorted() {
        val playlistId = 1L

        val recommended = listOf(
            PlaylistDto(
                id = 2L,
                title = "추천 플레이리스트1",
                description = "설명1",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            ),
            PlaylistDto(
                id = 3L,
                title = "추천 플레이리스트2",
                description = "설명2",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            )
        )

        whenever(playlistService.recommendPlaylist(playlistId, "likes")).thenReturn(recommended)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/playlists/{id}/recommendation", playlistId)
                .param("sortType", "likes")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("추천 플레이리스트 목록을 조회하였습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("추천 플레이리스트1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(3))

        verify(playlistService, times(1)).recommendPlaylist(playlistId, "likes")
    }

    @Test
    @DisplayName("사용자의 모든 플레이리스트를 조회할 수 있다.")
    fun getAllPlaylists() {
        // Given
        val playlists = listOf(
            PlaylistDto(
                id = 1L,
                title = "리스트1",
                description = "설명1",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = true
            ),
            PlaylistDto(
                id = 2L,
                title = "리스트2",
                description = "설명2",
                isPublic = false,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = true
            )
        )

        whenever(playlistService.getAllPlaylists()).thenReturn(playlists)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 목록 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("리스트1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].description").value("설명1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(2L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].title").value("리스트2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].description").value("설명2"))

        verify(playlistService, times(1)).getAllPlaylists()
    }


    @Test
    @DisplayName("특정 플레이리스트를 ID로 조회할 수 있다.")
    fun getPlaylistById() {
        // Given
        val playlistId = 1L

        val playlistDto = PlaylistDto(
            id = playlistId,
            title = "조회 플레이리스트",
            description = "조회 설명",
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = emptyList(),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(playlistService.getPlaylist(eq(playlistId), any())).thenReturn(playlistDto)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/{id}", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(playlistId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("조회 플레이리스트"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.description").value("조회 설명"))

        verify(playlistService, times(1)).getPlaylist(eq(playlistId), any())
    }

    @Test
    @DisplayName("공개 플레이리스트를 복제할 수 있다.")
    fun addPublicPlaylist() {
        // Given
        val playlistId = 1L

        val copiedPlaylist = PlaylistDto(
            id = 2L,
            title = "복제된 플레이리스트",
            description = "복제된 설명",
            isPublic = false,
            viewCount = 0,
            likeCount = 0,
            items = emptyList(),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(playlistService.addPublicPlaylist(playlistId)).thenReturn(copiedPlaylist)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/playlists/{id}", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트가 복제되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(2L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("복제된 플레이리스트"))

        verify(playlistService, times(1)).addPublicPlaylist(playlistId)
    }



    @Test
    @DisplayName("플레이리스트를 삭제할 수 있다.")
    fun deletePlaylist() {
        // Given
        val playlistId = 1L

        doNothing().whenever(playlistService).deletePlaylist(playlistId)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/playlists/{id}", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트가 삭제되었습니다."))

        verify(playlistService, times(1)).deletePlaylist(playlistId)
    }


    @Test
    @DisplayName("플레이리스트 정보를 수정할 수 있다.")
    fun updatePlaylist() {
        // Given
        val playlistId = 1L

        val request = PlaylistCreateDto(
            title = "수정된 제목",
            description = "수정된 설명",
            isPublic = false
        )

        val updatedDto = PlaylistDto(
            id = playlistId,
            title = request.title,
            description = request.description,
            isPublic = false,
            viewCount = 0,
            likeCount = 0,
            items = emptyList(),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(playlistService.updatePlaylist(eq(playlistId), any())).thenReturn(updatedDto)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/playlists/{id}", playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트가 수정되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(playlistId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("수정된 제목"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.description").value("수정된 설명"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isPublic").value(false))

        verify(playlistService, times(1)).updatePlaylist(eq(playlistId), any())
    }


    @Test
    @DisplayName("플레이리스트에 큐레이션을 추가할 수 있다.")
    fun addCurationToPlaylist() {
        // Given
        val playlistId = 1L
        val curationId = 99L

        val request = mapOf("curationId" to curationId.toString())

        val playlistItemDto = PlaylistItemDto(
            id = null,
            itemId = curationId,
            itemType = "CURATION",
            title = "큐레이션 제목",
            description = "큐레이션 설명",
            url = "",
            curationId = curationId,
            parentItemId = null
        )

        val playlistDto = PlaylistDto(
            id = playlistId,
            title = "테스트 플레이리스트",
            description = "설명",
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = listOf(playlistItemDto),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(
            playlistService.addPlaylistItem(
                eq(playlistId),
                eq(curationId),
                eq(PlaylistItem.PlaylistItemType.CURATION)
            )
        ).thenReturn(playlistDto)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/playlists/{id}/items/curation", playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트에 큐레이션이 추가되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(playlistId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].itemType").value("CURATION"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].itemId").value(curationId))

        verify(playlistService, times(1))
            .addPlaylistItem(playlistId, curationId, PlaylistItem.PlaylistItemType.CURATION)
    }


    @Test
    @DisplayName("플레이리스트아이템을 수정할 수 있다.")
    fun updatePlaylistItem() {
        // Given
        val playlistId = 1L
        val itemId = 100L

        val updateDto = PlaylistItemUpdateDto(
            title = "수정된 링크 제목",
            url = "https://updated-url.com",
            description = "수정된 설명"
        )

        val updatedItem = PlaylistItemDto(
            id = null,
            itemId = itemId,
            itemType = "LINK",
            title = updateDto.title,
            description = updateDto.description.orEmpty(),
            url = updateDto.url,
            curationId = null,
            parentItemId = null
        )

        val updatedDto = PlaylistDto(
            id = playlistId,
            title = "업데이트된 플레이리스트",
            description = "설명",
            isPublic = true,
            viewCount = 0,
            likeCount = 0,
            items = listOf(updatedItem),
            tags = emptySet(),
            createdAt = LocalDateTime.now(),
            isOwner = true
        )

        whenever(playlistService.updatePlaylistItem(eq(playlistId), eq(itemId), any()))
            .thenReturn(updatedDto)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/playlists/{id}/items/{itemId}", playlistId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 링크가 수정되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(playlistId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].itemId").value(itemId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value(updateDto.title))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].url").value(updateDto.url))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].description").value(updateDto.description))

        verify(playlistService, times(1)).updatePlaylistItem(eq(playlistId), eq(itemId), any())
    }

    @Test
    @DisplayName("로그인한 사용자의 좋아요 상태를 조회할 수 있다.")
    fun getLikeStatus_whenLoggedIn() {
        // Given
        val playlistId = 1L
        val loginId = 100L

        val mockMember = Member(
            id = 100L,
            username = "testuser",
            password = "testpass",
            role = RoleEnum.MEMBER
        )

        whenever(rq.isLogin).thenReturn(true)
        whenever(rq.actor).thenReturn(mockMember)
        whenever(playlistService.hasLikedPlaylist(playlistId, loginId)).thenReturn(true)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/{id}/like/status", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요 상태 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))

        verify(playlistService, times(1)).hasLikedPlaylist(playlistId, loginId)
    }


    @Test
    @DisplayName("비로그인 상태에서는 좋아요 상태가 false여야 한다.")
    fun getLikeStatus_whenNotLoggedIn() {
        // Given
        val playlistId = 1L

        whenever(rq.isLogin).thenReturn(false)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/{id}/like/status", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비로그인 상태입니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(false))

        verify(playlistService, never()).hasLikedPlaylist(any(), any())
    }


    @Test
    @DisplayName("플레이리스트의 좋아요 수를 조회할 수 있다.")
    fun getLikeCount() {
        // Given
        val playlistId = 1L
        val likeCount = 42L

        whenever(playlistService.getLikeCount(playlistId)).thenReturn(likeCount)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/{id}/like/count", playlistId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요 개수를 조회하였습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(42))

        verify(playlistService, times(1)).getLikeCount(playlistId)
    }


    @Test
    @DisplayName("사용자가 좋아요한 플레이리스트 목록을 조회할 수 있다.")
    fun getLikedPlaylists() {
        // Given
        val loginId = 100L

        val mockMember = Member(
            id = 100L,
            username = "testuser",
            password = "testpass",
            role = RoleEnum.MEMBER
        )

        whenever(rq.actor).thenReturn(mockMember)


        val likedPlaylists = listOf(
            PlaylistDto(
                id = 1L,
                title = "좋아요1",
                description = "설명1",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = true
            ),
            PlaylistDto(
                id = 2L,
                title = "좋아요2",
                description = "설명2",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = true
            )
        )

        whenever(playlistService.getLikedPlaylistsFromRedis(mockMember.id!!)).thenReturn(likedPlaylists)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/liked"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요한 플레이리스트 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("좋아요1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(2L))

        verify(playlistService, times(1)).getLikedPlaylistsFromRedis(loginId)
    }


    @Test
    @DisplayName("공개 플레이리스트 전체를 조회할 수 있다.")
    fun getAllPublicPlaylists() {
        // Given
        val publicPlaylists = listOf(
            PlaylistDto(
                id = 1L,
                title = "공개1",
                description = "설명1",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            ),
            PlaylistDto(
                id = 2L,
                title = "공개2",
                description = "설명2",
                isPublic = true,
                viewCount = 0,
                likeCount = 0,
                items = emptyList(),
                tags = emptySet(),
                createdAt = LocalDateTime.now(),
                isOwner = false
            )
        )

        whenever(playlistService.getAllPublicPlaylists()).thenReturn(publicPlaylists)

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/playlists/explore"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("공개 플레이리스트 전체 조회를 하였습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value("공개1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].isPublic").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(2L))

        verify(playlistService, times(1)).getAllPublicPlaylists()
    }

}