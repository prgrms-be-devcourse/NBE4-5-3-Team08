package com.team8.project2.domain.playlist.service;

import com.team8.project2.domain.link.service.LinkService;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.playlist.dto.*;
import com.team8.project2.domain.playlist.entity.Playlist;
import com.team8.project2.domain.playlist.entity.PlaylistItem;
import com.team8.project2.domain.playlist.repository.PlaylistLikeRepository;
import com.team8.project2.domain.playlist.repository.PlaylistRepository;
import com.team8.project2.global.Rq;
import com.team8.project2.global.exception.BadRequestException;
import com.team8.project2.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @InjectMocks
    private PlaylistService playlistService;

    @Mock
    private LinkService linkService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlaylistLikeRepository playlistLikeRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Rq rq;

    private Playlist samplePlaylist;
    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleMember = Member.builder()
                .id(1L)
                .username("테스트 유저")
                .email("test@example.com")
                .build();

        samplePlaylist = Playlist.builder()
                .id(1L)
                .title("테스트 플레이리스트")
                .tags(new HashSet<>())
                .description("테스트 설명")
                .likeCount(0L)
                .member(sampleMember)
                .build();

        lenient().when(memberRepository.findById(sampleMember.getId())).thenReturn(Optional.of(sampleMember));
        lenient().when(playlistRepository.findById(samplePlaylist.getId())).thenReturn(Optional.of(samplePlaylist));

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        lenient().when(rq.getActor()).thenReturn(sampleMember);
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 생성해야 한다.")
    void shouldCreatePlaylistSuccessfully() {
        // Given
        PlaylistCreateDto request = new PlaylistCreateDto();
        request.setTitle("새 플레이리스트");
        request.setDescription("새로운 설명");

        Playlist newPlaylist = Playlist.builder()
                .id(2L)
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(Set.of())
                .member(sampleMember)
                .build();

        when(playlistRepository.save(any(Playlist.class))).thenReturn(newPlaylist);

        // When
        PlaylistDto createdPlaylist = playlistService.createPlaylist(request);

        // Then
        assertNotNull(createdPlaylist);
        assertEquals(request.getTitle(), createdPlaylist.getTitle());
        assertEquals(request.getDescription(), createdPlaylist.getDescription());
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 조회해야 한다.")
    void shouldRetrievePlaylistSuccessfully() {
        // Given
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));

        // When
        PlaylistDto foundPlaylist = playlistService.getPlaylist(1L, new MockHttpServletRequest());

        // Then
        assertNotNull(foundPlaylist);
        assertEquals(samplePlaylist.getTitle(), foundPlaylist.getTitle());
        assertEquals(samplePlaylist.getDescription(), foundPlaylist.getDescription());
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트 조회 시 NotFoundException이 발생해야 한다.")
    void shouldThrowNotFoundExceptionWhenPlaylistDoesNotExist() {
        // Given
        when(playlistRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> playlistService.getPlaylist(99L, new MockHttpServletRequest()));
    }

    @Test
    @DisplayName("현재 로그인한 사용자의 모든 플레이리스트를 정상적으로 조회해야 한다.")
    void shouldRetrieveAllPlaylistsSuccessfully() {
        // Given
        List<Playlist> playlists = Arrays.asList(samplePlaylist);
        when(playlistRepository.findByMember(sampleMember)).thenReturn(playlists);

        // When
        List<PlaylistDto> foundPlaylists = playlistService.getAllPlaylists();

        // Then
        assertFalse(foundPlaylists.isEmpty());
        assertEquals(1, foundPlaylists.size());
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 수정해야 한다.")
    void shouldUpdatePlaylistSuccessfully() {
        // Given
        PlaylistUpdateDto request = new PlaylistUpdateDto();
        request.setTitle("수정된 플레이리스트");
        request.setDescription("수정된 설명");

        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);

        // When
        PlaylistDto updatedPlaylist = playlistService.updatePlaylist(1L, request);

        // Then
        assertNotNull(updatedPlaylist);
        assertEquals(request.getTitle(), updatedPlaylist.getTitle());
        assertEquals(request.getDescription(), updatedPlaylist.getDescription());
    }

    @Test
    @DisplayName("플레이리스트를 정상적으로 삭제해야 한다.")
    void shouldDeletePlaylistSuccessfully() {
        // Given
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
        doNothing().when(playlistRepository).deleteById(1L);

        // When & Then
        assertDoesNotThrow(() -> playlistService.deletePlaylist(1L));
        verify(playlistRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트 삭제 시 NotFoundException이 발생해야 한다.")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistingPlaylist() {
        // Given
        when(playlistRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> playlistService.deletePlaylist(99L));
    }

    @Test
    @DisplayName("플레이리스트에 아이템을 추가할 수 있다.")
    void addPlaylistItem() {
        // Given
        Long newItemId = 100L;

        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PlaylistDto updatedPlaylist = playlistService.addPlaylistItem(1L, newItemId, PlaylistItem.PlaylistItemType.LINK);

        // Then
        assertNotNull(updatedPlaylist);
        assertEquals("테스트 플레이리스트", updatedPlaylist.getTitle());
        assertFalse(updatedPlaylist.getItems().isEmpty());
        assertEquals(newItemId, updatedPlaylist.getItems().get(0).getItemId());
        assertEquals("LINK", updatedPlaylist.getItems().get(0).getItemType());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 플레이리스트에 아이템을 추가할 수 없다.")
    void addPlaylistItemNotFound() {
        // Given
        Long newItemId = 100L;
        when(playlistRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () ->
                playlistService.addPlaylistItem(1L, newItemId, PlaylistItem.PlaylistItemType.LINK));
    }

    @Test
    @DisplayName("플레이리스트에서 아이템을 삭제할 수 있다.")
    void deletePlaylistItem() {
        // Given
        Long itemDbIdToDelete = 1L;

        PlaylistItem item1 = PlaylistItem.builder()
                .id(1L)
                .itemId(100L)
                .itemType(PlaylistItem.PlaylistItemType.LINK)
                .build();

        PlaylistItem item2 = PlaylistItem.builder()
                .id(2L)
                .itemId(101L)
                .itemType(PlaylistItem.PlaylistItemType.CURATION)
                .build();

        samplePlaylist.setItems(new ArrayList<>(Arrays.asList(item1, item2)));
        when(playlistRepository.findById(samplePlaylist.getId())).thenReturn(Optional.of(samplePlaylist));

        // When
        playlistService.deletePlaylistItem(samplePlaylist.getId(), itemDbIdToDelete);

        // Then
        assertFalse(samplePlaylist.getItems().stream()
                .anyMatch(item -> item.getItemId().equals(itemDbIdToDelete)));
        verify(playlistRepository, times(1)).save(samplePlaylist);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 아이템은 삭제할 수 없다.")
    void deletePlaylistItemNotFound() {
        // Given
        Long itemIdToDelete = 100L;
        samplePlaylist.setItems(new ArrayList<>());

        when(playlistRepository.findById(samplePlaylist.getId()))
                .thenReturn(Optional.of(samplePlaylist));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            playlistService.deletePlaylistItem(samplePlaylist.getId(), itemIdToDelete);
        });
    }

    @Test
    @DisplayName("플레이리스트 아이템 순서를 변경할 수 있다.")
    void updatePlaylistItemOrder() {
        // Given
        PlaylistItem item1 = PlaylistItem.builder().id(1L).itemId(100L).displayOrder(0).itemType(PlaylistItem.PlaylistItemType.LINK).build();
        PlaylistItem item2 = PlaylistItem.builder().id(2L).itemId(101L).displayOrder(1).itemType(PlaylistItem.PlaylistItemType.CURATION).build();
        PlaylistItem item3 = PlaylistItem.builder().id(3L).itemId(102L).displayOrder(2).itemType(PlaylistItem.PlaylistItemType.LINK).build();
        samplePlaylist.setItems(new ArrayList<>(Arrays.asList(item1, item2, item3)));

        List<PlaylistItemOrderUpdateDto> newOrder = Arrays.asList(
                new PlaylistItemOrderUpdateDto(3L, new ArrayList<>()),
                new PlaylistItemOrderUpdateDto(1L, new ArrayList<>()),
                new PlaylistItemOrderUpdateDto(2L, new ArrayList<>())
        );


        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);

        // When
        PlaylistDto updatedDto = playlistService.updatePlaylistItemOrder(1L, newOrder);

        // Then
        assertEquals(0, samplePlaylist.getItems().stream().filter(item -> item.getId().equals(3L)).findFirst().get().getDisplayOrder());
        assertEquals(100, samplePlaylist.getItems().stream().filter(item -> item.getId().equals(1L)).findFirst().get().getDisplayOrder());
        assertEquals(200, samplePlaylist.getItems().stream().filter(item -> item.getId().equals(2L)).findFirst().get().getDisplayOrder());

        assertNotNull(updatedDto);
        assertEquals("테스트 플레이리스트", updatedDto.getTitle());
    }

    @Test
    @DisplayName("실패 - 플레이리스트 아이템 순서 변경 시 아이템 개수가 일치해야 한다.")
    void updatePlaylistItemOrder_itemCount() {
        // Given
        PlaylistItem item1 = PlaylistItem.builder().id(1L).itemId(100L).displayOrder(0).itemType(PlaylistItem.PlaylistItemType.LINK).build();
        PlaylistItem item2 = PlaylistItem.builder().id(2L).itemId(101L).displayOrder(1).itemType(PlaylistItem.PlaylistItemType.CURATION).build();
        PlaylistItem item3 = PlaylistItem.builder().id(3L).itemId(102L).displayOrder(2).itemType(PlaylistItem.PlaylistItemType.LINK).build();
        samplePlaylist.setItems(new ArrayList<>(Arrays.asList(item1, item2, item3)));

        List<PlaylistItemOrderUpdateDto> newOrder = Arrays.asList(
                new PlaylistItemOrderUpdateDto(3L, new ArrayList<>()),
                new PlaylistItemOrderUpdateDto(1L, new ArrayList<>())
        );


        when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));

        // When & Then
        assertThrows(BadRequestException.class, () ->
                playlistService.updatePlaylistItemOrder(1L, newOrder));
    }

    @Test
    @DisplayName("좋아요가 Redis에서 정상적으로 증가해야 한다.")
    void shouldIncreaseLikeCountInRedis() {
        Long playlistId = 1L;
        Long memberId = 1L;

        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("playlist_like:" + playlistId)), eq(String.valueOf(memberId))))
                .thenReturn(1L);
        when(setOperations.size("playlist_like:" + playlistId)).thenReturn(1L);

        // When
        playlistService.likePlaylist(playlistId, memberId);

        // Then
        verify(redisTemplate, times(1)).execute(any(DefaultRedisScript.class),
                eq(Collections.singletonList("playlist_like:" + playlistId)),
                eq(String.valueOf(memberId)));
        assertEquals(1L, samplePlaylist.getLikeCount());
    }

    @Test
    @DisplayName("추천 API가 Redis 캐싱을 사용하여 정상적으로 동작해야 한다.")
    void shouldRetrieveRecommendedPlaylistsFromCache() {
        Long playlistId = 1L;
        String cachedRecommendationsStr = "2,3";

        // Given - Redis에서 추천 데이터가 존재하는 경우
        when(valueOperations.get("playlist:recommend:" + playlistId)).thenReturn(cachedRecommendationsStr);
        when(playlistRepository.findAllById(Arrays.asList(2L, 3L)))
                .thenReturn(Arrays.asList(
                        Playlist.builder().id(2L).title("추천1").description("설명1").tags(new HashSet<>()).build(),
                        Playlist.builder().id(3L).title("추천2").description("설명2").tags(new HashSet<>()).build()
                ));

        // When
        List<PlaylistDto> recommendations = playlistService.recommendPlaylist(playlistId, "likes");

        // Then
        assertEquals(2, recommendations.size());
        verify(valueOperations, times(1)).get("playlist:recommend:" + playlistId);
        verify(playlistRepository, times(1)).findAllById(Arrays.asList(2L, 3L));
    }

    /**
     * ✅ 정렬별 추천 테스트
     */
    @Test
    @DisplayName("추천 플레이리스트가 좋아요 순으로 정렬되어야 한다.")
    void shouldSortRecommendedPlaylistsByLikes() {
        // Given
        Long playlistId = 1L;
        String sortType = "likes";

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist));

        // 추천 대상 플레이리스트 모킹 (likeCount 부여)
        Playlist p2 = Playlist.builder()
                .id(2L)
                .title("추천 플레이리스트1")
                .description("설명1")
                .tags(new HashSet<>())
                .likeCount(10L)
                .member(sampleMember)
                .build();
        Playlist p3 = Playlist.builder()
                .id(3L)
                .title("추천 플레이리스트2")
                .description("설명2")
                .tags(new HashSet<>())
                .likeCount(5L)
                .member(sampleMember)
                .build();

        List<Playlist> mockPlaylists = Arrays.asList(p2, p3);
        when(playlistRepository.findAllById(any())).thenReturn(mockPlaylists);

        when(valueOperations.get("playlist:recommend:" + playlistId)).thenReturn(null);

        // Redis 정렬된 집합 호출
        when(zSetOperations.reverseRange(eq("playlist:like_count:"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>(Arrays.asList("2", "3")));

        // Redis 나머지 빈 집합 설정
        when(zSetOperations.reverseRange(eq("playlist:view_count:"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());
        when(zSetOperations.reverseRange(eq("trending:24h"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());
        when(zSetOperations.reverseRange(eq("popular:24h"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());

        // 사용자의 플레이리스트 없음
        when(playlistRepository.findByMember(sampleMember)).thenReturn(Collections.emptyList());

        // When
        List<PlaylistDto> result = playlistService.recommendPlaylist(playlistId, sortType);

        // Then
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("추천 플레이리스트1", result.get(0).getTitle());
        assertEquals(3L, result.get(1).getId());
        assertEquals("추천 플레이리스트2", result.get(1).getTitle());

        verify(playlistRepository, times(1)).findById(playlistId);
    }


    @Test
    @DisplayName("추천 플레이리스트가 조회수 순으로 정렬되어야 한다.")
    void shouldSortRecommendedPlaylistsByViews() {
        // Given
        Long playlistId = 1L;
        String sortType = "views";

        Playlist samplePlaylist = Playlist.builder()
                .id(playlistId)
                .title("테스트 플레이리스트")
                .description("테스트 설명")
                .tags(new HashSet<>())
                .member(sampleMember)
                .build();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist));

        Playlist p2 = Playlist.builder()
                .id(2L)
                .title("추천1")
                .description("설명1")
                .tags(new HashSet<>())
                .viewCount(200L)
                .member(sampleMember)
                .build();
        Playlist p3 = Playlist.builder()
                .id(3L)
                .title("추천2")
                .description("설명2")
                .tags(new HashSet<>())
                .viewCount(100L)
                .member(sampleMember)
                .build();

        List<Playlist> mockPlaylists = Arrays.asList(p2, p3);
        when(playlistRepository.findAllById(any())).thenReturn(mockPlaylists);
        when(playlistRepository.findAll()).thenReturn(mockPlaylists);

        when(valueOperations.get("playlist:recommend:" + playlistId)).thenReturn(null);

        // Redis ZSet 모킹: 빈 집합
        when(zSetOperations.reverseRange(eq("playlist:view_count:"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());
        when(zSetOperations.reverseRange(eq("playlist:like_count:"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());
        when(zSetOperations.reverseRange(eq("trending:24h"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());
        when(zSetOperations.reverseRange(eq("popular:24h"), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());

        // 사용자의 플레이리스트 없음
        when(playlistRepository.findByMember(sampleMember)).thenReturn(Collections.emptyList());
        when(rq.getActor()).thenReturn(sampleMember);

        // When
        List<PlaylistDto> recommendations = playlistService.recommendPlaylist(playlistId, sortType);

        // Then
        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
        assertEquals(2L, recommendations.get(0).getId());
        assertEquals("추천1", recommendations.get(0).getTitle());
        assertEquals(3L, recommendations.get(1).getId());
        assertEquals("추천2", recommendations.get(1).getTitle());

        verify(playlistRepository, times(1)).findById(playlistId);
    }


    @Test
    @DisplayName("추천 플레이리스트가 좋아요+조회수 복합 점수 순으로 정렬되어야 한다.")
    void shouldSortRecommendedPlaylistsByCombined() {
        Long playlistId = 1L;
        String sortType = "combined";

        Member sampleMember = Member.builder()
                .id(1L)
                .username("테스트 유저")
                .email("test@example.com")
                .build();

        Playlist samplePlaylist = Playlist.builder()
                .id(playlistId)
                .title("테스트 플레이리스트")
                .description("테스트 설명")
                .tags(new HashSet<>())
                .member(sampleMember)
                .build();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist));

        Playlist p2 = Playlist.builder()
                .id(2L)
                .title("추천1")
                .description("설명1")
                .tags(new HashSet<>())
                .viewCount(150L)
                .likeCount(20L)
                .member(sampleMember)
                .build();
        Playlist p3 = Playlist.builder()
                .id(3L)
                .title("추천2")
                .description("설명2")
                .tags(new HashSet<>())
                .viewCount(100L)
                .likeCount(30L)
                .member(sampleMember)
                .build();

        List<Playlist> playlists = Arrays.asList(p2, p3);

        // findAllById()와 findAll()이 추천 대상 리스트를 반환하도록 설정
        when(playlistRepository.findAllById(any())).thenReturn(playlists);
        when(playlistRepository.findAll()).thenReturn(playlists);

        when(valueOperations.get("playlist:recommend:" + playlistId)).thenReturn(null);
        when(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .thenReturn(new HashSet<>());

        when(rq.getActor()).thenReturn(sampleMember);

        // When
        List<PlaylistDto> recommendations = playlistService.recommendPlaylist(playlistId, sortType);

        // Then
        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
        assertEquals(2L, recommendations.get(0).getId());
        assertEquals("추천1", recommendations.get(0).getTitle());
        assertEquals(3L, recommendations.get(1).getId());
        assertEquals("추천2", recommendations.get(1).getTitle());

        verify(playlistRepository, times(1)).findById(playlistId);
    }


    @Test
    @DisplayName("Redis 캐싱이 없을 때 추천 알고리즘을 실행해야 한다.")
    void shouldRunRecommendationAlgorithmIfCacheMiss() {
        // Given
        Long playlistId = 1L;
        String sortType = "combined";

        Member sampleMember = Member.builder()
                .id(1L)
                .username("테스트 유저")
                .email("test@example.com")
                .build();
        Playlist samplePlaylist = Playlist.builder()
                .id(playlistId)
                .title("테스트 플레이리스트")
                .description("테스트 설명")
                .tags(new HashSet<>())
                .member(sampleMember)
                .build();

        when(valueOperations.get("playlist:recommend:" + playlistId)).thenReturn(null);

        Set<Object> trendingPlaylists = new HashSet<>(Arrays.asList("2", "3"));
        Set<Object> popularPlaylists = new HashSet<>(Arrays.asList("3", "4"));

        doReturn(trendingPlaylists)
                .when(zSetOperations)
                .reverseRange(eq("playlist:view_count:"), eq(0L), eq(5L));
        doReturn(popularPlaylists)
                .when(zSetOperations)
                .reverseRange(eq("playlist:like_count:"), eq(0L), eq(5L));

        doReturn(trendingPlaylists)
                .when(zSetOperations)
                .reverseRange(eq("trending:24h"), eq(0L), eq(5L));
        doReturn(popularPlaylists)
                .when(zSetOperations)
                .reverseRange(eq("popular:24h"), eq(0L), eq(5L));

        List<Long> recommendedPlaylistIds = Arrays.asList(2L, 3L, 4L);
        List<Playlist> mockPlaylists = Arrays.asList(
                Playlist.builder().id(2L).title("추천1").description("설명1")
                        .tags(new HashSet<>()).member(sampleMember).build(),
                Playlist.builder().id(3L).title("추천2").description("설명2")
                        .tags(new HashSet<>()).member(sampleMember).build(),
                Playlist.builder().id(4L).title("추천3").description("설명3")
                        .tags(new HashSet<>()).member(sampleMember).build()
        );

        when(playlistRepository.findAllById(any())).thenReturn(mockPlaylists);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(samplePlaylist));
        when(rq.getActor()).thenReturn(sampleMember);

        // When
        List<PlaylistDto> recommendations = playlistService.recommendPlaylist(playlistId, sortType);

        // Then
        assertEquals(3, recommendations.size());
        verify(valueOperations, times(1))
                .set(eq("playlist:recommend:" + playlistId), any(), any());
    }


    @DisplayName("공개 플레이리스트를 내 플레이리스트로 복사한다.")
    @Test
    void addPublicPlaylistToMyPlaylist() {
        // given
        Member member = Member.builder().id(1L).username("testUser").build();
        Playlist originalPlaylist = Playlist.builder()
                .id(100L)
                .title("공개 플레이리스트")
                .description("공개 설명")
                .isPublic(true)
                .items(new ArrayList<>())
                .member(Member.builder().id(2L).build())
                .build();

        given(rq.getActor()).willReturn(member);
        given(playlistRepository.findById(originalPlaylist.getId()))
                .willReturn(Optional.of(originalPlaylist));
        given(playlistRepository.save(any(Playlist.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlaylistDto result = playlistService.addPublicPlaylist(originalPlaylist.getId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.isOwner()).isTrue();
        assertThat(result.getTitle()).isEqualTo(originalPlaylist.getTitle());
        assertThat(result.getDescription()).isEqualTo(originalPlaylist.getDescription());
        assertThat(result.isPublic()).isFalse();
        assertThat(result.getItems()).hasSize(originalPlaylist.getItems().size());
    }

    @DisplayName("플레이리스트 아이템의 내용을 수정한다")
    @Test
    void updatePlaylistLinkItemContent() {
        // given
        Long playlistId = 1L;
        Long playlistItemId = 10L;
        Long linkId = 100L;

        Member member = Member.builder()
                .id(1L)
                .username("testUser")
                .build();

        Link link = Link.builder()
                .id(linkId)
                .title("기존 제목")
                .url("https://old-url.com")
                .description("기존 설명")
                .build();

        PlaylistItem playlistItem = PlaylistItem.builder()
                .id(playlistItemId)
                .itemId(linkId)
                .itemType(PlaylistItem.PlaylistItemType.LINK)
                .displayOrder(0)
                .link(link)
                .build();

        Playlist playlist = Playlist.builder()
                .id(playlistId)
                .title("수정 가능한 플리")
                .member(member)
                .items(new ArrayList<>(List.of(playlistItem)))
                .build();

        PlaylistItemUpdateDto updateDto = PlaylistItemUpdateDto.builder()
                .title("수정된 링크 제목")
                .description("수정된 링크 설명")
                .url("https://new-url.com")
                .build();

        given(rq.getActor()).willReturn(member);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        lenient().when(playlistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        given(linkService.updateLinkDetails(eq(linkId), eq(updateDto.getTitle()), eq(updateDto.getUrl()), eq(updateDto.getDescription())))
                .willReturn(Link.builder()
                        .id(linkId)
                        .title(updateDto.getTitle())
                        .url(updateDto.getUrl())
                        .description(updateDto.getDescription())
                        .build());

        // when
        PlaylistDto result = playlistService.updatePlaylistItem(playlistId, playlistItemId, updateDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        PlaylistItemDto updatedItem = result.getItems().get(0);
        assertThat(updatedItem.getItemType()).isEqualTo("LINK");
        assertThat(updatedItem.getUrl()).isEqualTo("https://new-url.com");
        assertThat(updatedItem.getTitle()).isEqualTo("수정된 링크 제목");
        assertThat(updatedItem.getDescription()).isEqualTo("수정된 링크 설명");
    }

    @DisplayName("현재 로그인한 사용자의 좋아요 여부를 확인한다")
    @Test
    void checkUserLikedPlaylist() {
        // given
        Long playlistId = 1L;
        Long memberId = 1L;
        String redisKey = "playlist_like:" + playlistId;

        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember(redisKey, String.valueOf(memberId))).willReturn(true);

        // when
        boolean isLiked = playlistService.hasLikedPlaylist(playlistId, memberId);

        // then
        assertThat(isLiked).isTrue();
        verify(setOperations, times(1)).isMember(redisKey, String.valueOf(memberId));
    }


    @DisplayName("플레이리스트의 좋아요 개수를 조회한다")
    @Test
    void getPlaylistLikeCount() {
        // given
        Long playlistId = 1L;
        String redisKey = "playlist_like:" + playlistId;

        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.size(redisKey)).willReturn(42L);

        // when
        long likeCount = playlistService.getLikeCount(playlistId);

        // then
        assertThat(likeCount).isEqualTo(42L);
        verify(setOperations, times(1)).size(redisKey);
    }


    @DisplayName("현재 로그인한 사용자가 좋아요한 플레이리스트 목록 조회한다")
    @Test
    void getLikedPlaylists() {
        // given
        Long memberId = 1L;
        String redisKey = "member_liked_playlists:" + memberId;

        Playlist playlist1 = Playlist.builder()
                .id(10L)
                .title("좋아요한 첫 번째 플레이리스트")
                .member(sampleMember)
                .build();

        Playlist playlist2 = Playlist.builder()
                .id(20L)
                .title("좋아요한 두 번째 플레이리스트")
                .member(sampleMember)
                .build();

        Set<Object> likedIds = new HashSet<>(Arrays.asList("10", "20"));

        given(rq.getActor()).willReturn(sampleMember);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members(redisKey)).willReturn(likedIds);
        given(playlistRepository.findAllById(anyList())).willReturn(List.of(playlist1, playlist2));

        // when
        List<PlaylistDto> result = playlistService.getLikedPlaylistsFromRedis(memberId);

        // then
        assertThat(result).hasSize(2);
        List<Long> ids = result.stream().map(PlaylistDto::getId).toList();
        assertThat(ids).containsExactlyInAnyOrder(10L, 20L);

        verify(setOperations, times(1)).members(redisKey);
        verify(playlistRepository, times(1)).findAllById(anyList());
    }


    @DisplayName("공개된 전체 플레이리스트를 조회한다")
    @Test
    void getAllPublicPlaylists() {
        // given
        Member actor = sampleMember;
        Playlist playlist1 = Playlist.builder()
                .id(1L)
                .title("공개 플레이리스트 1")
                .isPublic(true)
                .member(actor)
                .build();

        Playlist playlist2 = Playlist.builder()
                .id(2L)
                .title("공개 플레이리스트 2")
                .isPublic(true)
                .member(actor)
                .build();

        given(rq.isLogin()).willReturn(true);
        given(rq.getActor()).willReturn(actor);
        given(playlistRepository.findAllByIsPublicTrue())
                .willReturn(List.of(playlist1, playlist2));

        // when
        List<PlaylistDto> result = playlistService.getAllPublicPlaylists();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(playlistRepository, times(1)).findAllByIsPublicTrue();
    }

}