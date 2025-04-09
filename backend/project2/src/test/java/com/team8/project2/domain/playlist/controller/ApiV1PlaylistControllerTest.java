package com.team8.project2.domain.playlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.project2.domain.link.dto.LinkReqDTO;
import com.team8.project2.domain.link.service.LinkService;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.playlist.dto.*;
import com.team8.project2.domain.playlist.entity.PlaylistItem;
import com.team8.project2.domain.playlist.service.PlaylistService;
import com.team8.project2.global.Rq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiV1PlaylistControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApiV1PlaylistController playlistController;

    @Mock
    private PlaylistService playlistService;

    @Autowired
    private MemberRepository memberRepository;

    @Mock
    private Rq rq;

    @Mock
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(playlistController).build();
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 생성해야 한다.")
    void shouldCreatePlaylistSuccessfully() throws Exception {
        // Given
        PlaylistCreateDto request = new PlaylistCreateDto();
        request.setTitle("New Playlist");
        request.setDescription("Description");

        PlaylistDto response = PlaylistDto.builder()
                .id(1L)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        when(playlistService.createPlaylist(any(PlaylistCreateDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Playlist"));
    }

    @DisplayName("플레이리스트에 링크 추가가 정상적으로 이루어져야 한다.")
    @Test
    void addLinkToPlaylist() throws Exception {
        // Given
        Long playlistId = 1L;
        Long linkId = 100L;

        LinkReqDTO linkReqDTO = new LinkReqDTO(
                "https://example.com",
                "테스트 링크",
                "링크 설명"
        );


        Link dummyLink = Link.builder()
                .id(linkId)
                .title(linkReqDTO.getTitle())
                .url(linkReqDTO.getUrl())
                .description(linkReqDTO.getDescription())
                .build();

        PlaylistDto playlistDto = PlaylistDto.builder()
                .id(playlistId)
                .title("테스트 플레이리스트")
                .description("설명")
                .isPublic(true)
                .items(List.of(
                        PlaylistItemDto.builder()
                                .itemId(linkId)
                                .itemType("LINK")
                                .title(dummyLink.getTitle())
                                .url(dummyLink.getUrl())
                                .description(dummyLink.getDescription())
                                .build()
                ))
                .build();

        when(linkService.addLink(any(LinkReqDTO.class))).thenReturn(dummyLink);
        when(playlistService.addPlaylistItem(eq(playlistId), eq(linkId), eq(PlaylistItem.PlaylistItemType.LINK)))
                .thenReturn(playlistDto);

        // When & Then
        mockMvc.perform(post("/api/v1/playlists/{id}/items/link", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("테스트 플레이리스트"))
                .andExpect(jsonPath("$.data.items[0].itemType").value("LINK"))
                .andExpect(jsonPath("$.data.items[0].itemId").value(linkId));
    }


    @Test
    @DisplayName("플레이리스트에서 아이템이 삭제되어야 한다.")
    void deletePlaylistItem() throws Exception {
        Long playlistId = 1L;
        Long itemId = 100L;

        mockMvc.perform(delete("/api/v1/playlists/{id}/items/{itemId}", playlistId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트 아이템이 삭제되었습니다."));

        verify(playlistService, times(1)).deletePlaylistItem(playlistId, itemId);
    }


    @Test
    @DisplayName("플레이리스트에서 아이템 순서가 변경되어야 한다.")
    void updatePlaylistItemOrder() throws Exception {
        PlaylistDto updatedDto = PlaylistDto.builder()
                .id(1L)
                .title("테스트 플레이리스트")
                .description("테스트 설명")
                .build();

        List<PlaylistItemOrderUpdateDto> newOrder = Arrays.asList(
                new PlaylistItemOrderUpdateDto(3L, null),
                new PlaylistItemOrderUpdateDto(1L, null),
                new PlaylistItemOrderUpdateDto(2L, null)
        );

        when(playlistService.updatePlaylistItemOrder(eq(1L), anyList()))
                .thenReturn(updatedDto);

        String jsonContent = "[{\"id\":3},{\"id\":1},{\"id\":2}]";

        mockMvc.perform(patch("/api/v1/playlists/1/items/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트 아이템 순서가 변경되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 플레이리스트"));
    }


    /**
     * ✅ 좋아요 증가 API 테스트
     */
    @DisplayName("좋아요 증가 API가 정상적으로 호출되어야 한다.")
    @Test
    void shouldIncreaseLikeCount() throws Exception {
        // given
        Long playlistId = 1L;
        Long memberId = 100L;

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);

        given(rq.getActor()).willReturn(mockMember);
        doNothing().when(playlistService).likePlaylist(playlistId, memberId);

        // when & then
        mockMvc.perform(post("/api/v1/playlists/{id}/like", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("좋아요 상태가 토글되었습니다."));

        verify(playlistService, times(1)).likePlaylist(playlistId, memberId);
    }

    /**
     * ✅ 추천 API 테스트
     */
    @Test
    @DisplayName("플레이리스트의 추천 목록을 정렬하여 조회할 수 있다.")
    void getRecommendedPlaylistsWithSorting() throws Exception {
        Long playlistId = 1L;

        // ✅ 정렬 기준 추가
        String sortType1 = "views";
        String sortType2 = "likes";
        String sortType3 = "combined";

        List<PlaylistDto> recommended = List.of(
                PlaylistDto.builder().id(2L).title("추천 플레이리스트1").description("설명1").build(),
                PlaylistDto.builder().id(3L).title("추천 플레이리스트2").description("설명2").build()
        );

        // ✅ 모든 정렬 옵션에 대해 Stubbing 설정
        when(playlistService.recommendPlaylist(playlistId, sortType1)).thenReturn(recommended);
        when(playlistService.recommendPlaylist(playlistId, sortType2)).thenReturn(recommended);
        when(playlistService.recommendPlaylist(playlistId, sortType3)).thenReturn(recommended);

        // ✅ "views" 정렬 기준으로 요청 (컨트롤러에서 `combined`이 기본값이므로, 명시적으로 요청해야 함)
        mockMvc.perform(get("/api/v1/playlists/{id}/recommendation", playlistId)
                        .param("sortType", sortType1) // views
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("추천 플레이리스트1"))
                .andExpect(jsonPath("$.data[1].title").value("추천 플레이리스트2"));

        // ✅ "likes" 정렬 기준으로 요청
        mockMvc.perform(get("/api/v1/playlists/{id}/recommendation", playlistId)
                        .param("sortType", sortType2) // likes
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("추천 플레이리스트1"))
                .andExpect(jsonPath("$.data[1].title").value("추천 플레이리스트2"));

        // ✅ "combined" 정렬 기준으로 요청
        mockMvc.perform(get("/api/v1/playlists/{id}/recommendation", playlistId)
                        .param("sortType", sortType3) // combined
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("추천 플레이리스트1"))
                .andExpect(jsonPath("$.data[1].title").value("추천 플레이리스트2"));

        // ✅ verify를 통해 모든 정렬 옵션에 대한 호출을 검증
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType1);
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType2);
        verify(playlistService, times(1)).recommendPlaylist(playlistId, sortType3);
    }

    @Test
    @DisplayName("플레이리스트 추천 기능이 정렬 기준에 따라 정상 동작해야 한다.")
    void shouldReturnRecommendedPlaylistsSorted() throws Exception {
        Long playlistId = 1L;
        List<PlaylistDto> recommended = List.of(
                PlaylistDto.builder().id(2L).title("추천 플레이리스트1").description("설명1").build(),
                PlaylistDto.builder().id(3L).title("추천 플레이리스트2").description("설명2").build()
        );

        when(playlistService.recommendPlaylist(playlistId, "likes")).thenReturn(recommended);

        mockMvc.perform(get("/api/v1/playlists/{id}/recommendation", playlistId)
                        .param("sortType", "likes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("추천 플레이리스트 목록을 조회하였습니다."))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].title").value("추천 플레이리스트1"))
                .andExpect(jsonPath("$.data[1].id").value(3));

        verify(playlistService, times(1)).recommendPlaylist(playlistId, "likes");
    }


    @Test
    @DisplayName("사용자의 모든 플레이리스트를 조회할 수 있다.")
    void getAllPlaylists() throws Exception {
        // Given
        List<PlaylistDto> playlists = List.of(
                PlaylistDto.builder()
                        .id(1L)
                        .title("리스트1")
                        .description("설명1")
                        .isPublic(true)
                        .build(),
                PlaylistDto.builder()
                        .id(2L)
                        .title("리스트2")
                        .description("설명2")
                        .isPublic(false)
                        .build()
        );

        when(playlistService.getAllPlaylists()).thenReturn(playlists);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("리스트1"))
                .andExpect(jsonPath("$.data[0].description").value("설명1"))
                .andExpect(jsonPath("$.data[1].id").value(2L))
                .andExpect(jsonPath("$.data[1].title").value("리스트2"))
                .andExpect(jsonPath("$.data[1].description").value("설명2"));

        verify(playlistService, times(1)).getAllPlaylists();
    }

    @Test
    @DisplayName("특정 플레이리스트를 ID로 조회할 수 있다.")
    void getPlaylistById() throws Exception {
        // Given
        Long playlistId = 1L;

        PlaylistDto playlistDto = PlaylistDto.builder()
                .id(playlistId)
                .title("조회 플레이리스트")
                .description("조회 설명")
                .isPublic(true)
                .build();

        when(playlistService.getPlaylist(eq(playlistId), any())).thenReturn(playlistDto);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/{id}", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(playlistId))
                .andExpect(jsonPath("$.data.title").value("조회 플레이리스트"))
                .andExpect(jsonPath("$.data.description").value("조회 설명"));

        verify(playlistService, times(1)).getPlaylist(eq(playlistId), any());
    }

    @Test
    @DisplayName("공개 플레이리스트를 복제할 수 있다.")
    void addPublicPlaylist() throws Exception {
        // Given
        Long playlistId = 1L;

        PlaylistDto copiedPlaylist = PlaylistDto.builder()
                .id(2L)
                .title("복제된 플레이리스트")
                .description("복제된 설명")
                .isPublic(false)
                .build();

        when(playlistService.addPublicPlaylist(playlistId)).thenReturn(copiedPlaylist);

        // When & Then
        mockMvc.perform(post("/api/v1/playlists/{id}", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트가 복제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.title").value("복제된 플레이리스트"));

        verify(playlistService, times(1)).addPublicPlaylist(playlistId);
    }


    @Test
    @DisplayName("플레이리스트를 삭제할 수 있다.")
    void deletePlaylist() throws Exception {
        // Given
        Long playlistId = 1L;

        doNothing().when(playlistService).deletePlaylist(playlistId);

        // When & Then
        mockMvc.perform(delete("/api/v1/playlists/{id}", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트가 삭제되었습니다."));

        verify(playlistService, times(1)).deletePlaylist(playlistId);
    }

    @Test
    @DisplayName("플레이리스트 정보를 수정할 수 있다.")
    void updatePlaylist() throws Exception {
        // Given
        Long playlistId = 1L;

        PlaylistCreateDto request = new PlaylistCreateDto();
        request.setTitle("수정된 제목");
        request.setDescription("수정된 설명");
        request.setIsPublic(false);

        PlaylistDto updatedDto = PlaylistDto.builder()
                .id(playlistId)
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(false)
                .build();

        when(playlistService.updatePlaylist(eq(playlistId), any())).thenReturn(updatedDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/playlists/{id}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트가 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(playlistId))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"))
                .andExpect(jsonPath("$.data.isPublic").value(false));

        verify(playlistService, times(1)).updatePlaylist(eq(playlistId), any());
    }


    @Test
    @DisplayName("플레이리스트에 큐레이션을 추가할 수 있다.")
    void addCurationToPlaylist() throws Exception {
        // Given
        Long playlistId = 1L;
        Long curationId = 99L;

        Map<String, String> request = new HashMap<>();
        request.put("curationId", String.valueOf(curationId));

        PlaylistDto playlistDto = PlaylistDto.builder()
                .id(playlistId)
                .title("테스트 플레이리스트")
                .description("설명")
                .isPublic(true)
                .items(List.of(
                        PlaylistItemDto.builder()
                                .itemId(curationId)
                                .itemType("CURATION")
                                .title("큐레이션 제목")
                                .description("큐레이션 설명")
                                .build()
                ))
                .build();

        when(playlistService.addPlaylistItem(eq(playlistId), eq(curationId), eq(PlaylistItem.PlaylistItemType.CURATION)))
                .thenReturn(playlistDto);

        // When & Then
        mockMvc.perform(post("/api/v1/playlists/{id}/items/curation", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트에 큐레이션이 추가되었습니다."))
                .andExpect(jsonPath("$.data.id").value(playlistId))
                .andExpect(jsonPath("$.data.items[0].itemType").value("CURATION"))
                .andExpect(jsonPath("$.data.items[0].itemId").value(curationId));

        verify(playlistService, times(1))
                .addPlaylistItem(playlistId, curationId, PlaylistItem.PlaylistItemType.CURATION);
    }

    @Test
    @DisplayName("플레이리스트아이템을 수정할 수 있다.")
    void updatePlaylistItem() throws Exception {
        // Given
        Long playlistId = 1L;
        Long itemId = 100L;

        PlaylistItemUpdateDto updateDto = PlaylistItemUpdateDto.builder()
                .title("수정된 링크 제목")
                .url("https://updated-url.com")
                .description("수정된 설명")
                .build();

        PlaylistDto updatedDto = PlaylistDto.builder()
                .id(playlistId)
                .title("업데이트된 플레이리스트")
                .description("설명")
                .items(List.of(
                        PlaylistItemDto.builder()
                                .itemId(itemId)
                                .itemType("LINK")
                                .title(updateDto.getTitle())
                                .url(updateDto.getUrl())
                                .description(updateDto.getDescription())
                                .build()
                ))
                .build();

        when(playlistService.updatePlaylistItem(eq(playlistId), eq(itemId), any(PlaylistItemUpdateDto.class)))
                .thenReturn(updatedDto);

        // When & Then
        mockMvc.perform(patch("/api/v1/playlists/{id}/items/{itemId}", playlistId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("플레이리스트 링크가 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(playlistId))
                .andExpect(jsonPath("$.data.items[0].itemId").value(itemId))
                .andExpect(jsonPath("$.data.items[0].title").value(updateDto.getTitle()))
                .andExpect(jsonPath("$.data.items[0].url").value(updateDto.getUrl()))
                .andExpect(jsonPath("$.data.items[0].description").value(updateDto.getDescription()));

        verify(playlistService, times(1))
                .updatePlaylistItem(eq(playlistId), eq(itemId), any(PlaylistItemUpdateDto.class));
    }

    @Test
    @DisplayName("로그인한 사용자의 좋아요 상태를 조회할 수 있다.")
    void getLikeStatus_whenLoggedIn() throws Exception {
        // Given
        Long playlistId = 1L;
        Long memberId = 100L;

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);

        when(rq.isLogin()).thenReturn(true);
        when(rq.getActor()).thenReturn(mockMember);
        when(playlistService.hasLikedPlaylist(playlistId, memberId)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/{id}/like/status", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("좋아요 상태 조회 성공"))
                .andExpect(jsonPath("$.data").value(true));

        verify(playlistService, times(1)).hasLikedPlaylist(playlistId, memberId);
    }

    @Test
    @DisplayName("비로그인 상태에서는 좋아요 상태가 false여야 한다.")
    void getLikeStatus_whenNotLoggedIn() throws Exception {
        // Given
        Long playlistId = 1L;

        when(rq.isLogin()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/{id}/like/status", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("비로그인 상태입니다."))
                .andExpect(jsonPath("$.data").value(false));

        verify(playlistService, never()).hasLikedPlaylist(anyLong(), anyLong());
    }

    @Test
    @DisplayName("플레이리스트의 좋아요 수를 조회할 수 있다.")
    void getLikeCount() throws Exception {
        // Given
        Long playlistId = 1L;
        long likeCount = 42L;

        when(playlistService.getLikeCount(playlistId)).thenReturn(likeCount);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/{id}/like/count", playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("좋아요 개수를 조회하였습니다."))
                .andExpect(jsonPath("$.data").value(42));

        verify(playlistService, times(1)).getLikeCount(playlistId);
    }

    @Test
    @DisplayName("사용자가 좋아요한 플레이리스트 목록을 조회할 수 있다.")
    void getLikedPlaylists() throws Exception {
        // Given
        Long memberId = 123L;

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(memberId);
        given(rq.getActor()).willReturn(mockMember);

        List<PlaylistDto> likedPlaylists = List.of(
                PlaylistDto.builder().id(1L).title("좋아요1").description("설명1").build(),
                PlaylistDto.builder().id(2L).title("좋아요2").description("설명2").build()
        );

        when(playlistService.getLikedPlaylistsFromRedis(memberId)).thenReturn(likedPlaylists);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/liked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("좋아요한 플레이리스트 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("좋아요1"))
                .andExpect(jsonPath("$.data[1].id").value(2L));

        verify(playlistService, times(1)).getLikedPlaylistsFromRedis(memberId);
    }

    @Test
    @DisplayName("공개 플레이리스트 전체를 조회할 수 있다.")
    void getAllPublicPlaylists() throws Exception {
        // Given
        List<PlaylistDto> publicPlaylists = List.of(
                PlaylistDto.builder().id(1L).title("공개1").description("설명1").isPublic(true).build(),
                PlaylistDto.builder().id(2L).title("공개2").description("설명2").isPublic(true).build()
        );

        when(playlistService.getAllPublicPlaylists()).thenReturn(publicPlaylists);

        // When & Then
        mockMvc.perform(get("/api/v1/playlists/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("공개 플레이리스트 전체 조회를 하였습니다."))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("공개1"))
                .andExpect(jsonPath("$.data[0].isPublic").value(true))
                .andExpect(jsonPath("$.data[1].id").value(2L));

        verify(playlistService, times(1)).getAllPublicPlaylists();
    }


}
