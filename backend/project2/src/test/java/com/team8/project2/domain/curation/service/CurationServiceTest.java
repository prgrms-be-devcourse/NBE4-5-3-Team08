package com.team8.project2.domain.curation.service;

import com.team8.project2.domain.curation.curation.dto.CurationDetailResDto;
import com.team8.project2.domain.curation.curation.dto.CurationResDto;
import com.team8.project2.domain.curation.curation.dto.TrendingCurationResDto;
import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.curation.entity.CurationLink;
import com.team8.project2.domain.curation.curation.entity.CurationTag;
import com.team8.project2.domain.curation.curation.entity.SearchOrder;
import com.team8.project2.domain.curation.curation.event.CurationUpdateEvent;
import com.team8.project2.domain.curation.curation.repository.CurationLinkRepository;
import com.team8.project2.domain.curation.curation.repository.CurationRepository;
import com.team8.project2.domain.curation.curation.repository.CurationTagRepository;

import com.team8.project2.domain.curation.curation.service.CurationService;
import com.team8.project2.domain.curation.curation.service.CurationViewService;
import com.team8.project2.domain.curation.like.entity.Like;
import com.team8.project2.domain.curation.like.repository.LikeRepository;
import com.team8.project2.domain.curation.report.entity.ReportType;
import com.team8.project2.domain.curation.report.repository.ReportRepository;
import com.team8.project2.domain.curation.tag.entity.Tag;
import com.team8.project2.domain.curation.tag.service.TagService;
import com.team8.project2.domain.image.repository.CurationImageRepository;
import com.team8.project2.domain.image.service.CurationImageService;
import com.team8.project2.domain.link.entity.Link;
import com.team8.project2.domain.link.service.LinkService;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.member.service.MemberService;
import com.team8.project2.global.Rq;
import com.team8.project2.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class CurationServiceTest {

	@Mock
	private CurationRepository curationRepository;

	@Mock
	private CurationLinkRepository curationLinkRepository;

	@Mock
	private CurationTagRepository curationTagRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private LikeRepository likeRepository;

	@Mock
	private LinkService linkService;

	@Mock
	private TagService tagService;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private MemberService memberService;

	@Mock
	private CurationViewService  curationViewService;

	@Mock
	private CurationImageService curationImageService;

	@Mock
	private CurationImageRepository curationImageRepository;

	@Mock
	private Rq rq;

	@InjectMocks
	private CurationService curationService;

	private Curation curation;
	private Link link;
	private Tag tag;
	private Member member;

	@BeforeEach
	public void setup() {
		member = Member.builder()
				.id(1L)
				.username("testUser")
				.email("test@example.com")
				.build();


		curation = new Curation(
				"Test Title",
				"Test Content",
				member
		);

		link = Link.builder()
				.id(1L)
				.url("https://test.com")
				.build();

		tag = new Tag("test");

	}


	@Test
	@DisplayName("큐레이션을 생성할 수 있다")
	void createCuration() {
		List<String> urls = Arrays.asList("http://example.com", "http://another-url.com");
		List<String> tags = Arrays.asList("tag1", "tag2", "tag3");

		// Mocking repository and service calls
		when(linkService.getLink(anyString())).thenReturn(link);
		when(tagService.getTag(anyString())).thenReturn(tag);
		when(curationRepository.save(any(Curation.class))).thenReturn(curation);
		when(curationLinkRepository.saveAll(ArgumentMatchers.anyList())).thenReturn(List.of(new CurationLink()));
		when(curationTagRepository.saveAll(ArgumentMatchers.anyList())).thenReturn(List.of(new CurationTag()));

		Curation createdCuration = curationService.createCuration("New Title", "New Content", urls, tags, new Member());

		// Verify interactions
		verify(curationRepository, times(1)).save(any(Curation.class));
		verify(curationLinkRepository, times(1)).saveAll(ArgumentMatchers.anyList());
		verify(curationTagRepository, times(1)).saveAll(ArgumentMatchers.anyList());

		// Check the result
		assert createdCuration != null;
		assert createdCuration.getTitle().equals("New Title");
	}

	@Test
	@DisplayName("큐레이션을 수정할 수 있다")
	void UpdateCuration() {


		// Given: 테스트를 위한 데이터 준비
		List<String> urls = Arrays.asList("http://updated-url.com", "http://another-url.com");
		List<String> tags = Arrays.asList("updated-tag1", "updated-tag2", "updated-tag3");

		// Mocking Curation 객체
		Curation curation = new Curation(
				"Original Title",
				"Original Content",
				member
		);

		// Mocking 링크 및 태그
		Link link = new Link();  // Link 객체를 생성하는 코드 필요 (예: getLink 메서드에서 반환할 객체 설정)
		Tag tag = new Tag();     // Tag 객체 생성 (예: getTag 메서드에서 반환할 객체 설정)

		// Mocking 리포지토리 및 서비스 호출
		when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));
		when(linkService.getLink(anyString())).thenReturn(link);
		when(tagService.getTag(anyString())).thenReturn(tag);
		when(curationRepository.save(any(Curation.class))).thenReturn(curation);
		when(curationLinkRepository.saveAll(ArgumentMatchers.anyList())).thenReturn(List.of(new CurationLink()));
		when(curationTagRepository.saveAll(ArgumentMatchers.anyList())).thenReturn(List.of(new CurationTag()));
		doNothing().when(eventPublisher).publishEvent(any(CurationUpdateEvent.class));

		// When: 큐레이션 업데이트 호출
		Curation updatedCuration = curationService.updateCuration(1L, "Updated Title", "Updated Content", urls, tags, member);

		// Then: 상호작용 검증
		verify(curationRepository, times(1)).findById(1L);
		verify(curationRepository, times(1)).save(any(Curation.class));
		verify(curationLinkRepository, times(1)).saveAll(ArgumentMatchers.anyList());
		verify(curationTagRepository, times(1)).saveAll(ArgumentMatchers.anyList());

		// 결과 확인
		assertNotNull(updatedCuration);
		assertEquals("Updated Title", updatedCuration.getTitle());
		assertEquals("Updated Content", updatedCuration.getContent());
	}



	@Test
	@DisplayName("실패 - 존재하지 않는 큐레이션을 수정하면 실패한다")
	void UpdateCurationNotFound() {
		Member member = new Member(); // Member 객체 생성
		List<String> urls = Arrays.asList("http://updated-url.com");
		List<String> tags = Arrays.asList("tag1", "tag2", "tag3");

		// Mocking repository to return empty Optional
		when(curationRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Check if exception is thrown
		try {
			curationService.updateCuration(1L, "Updated Title", "Updated Content", urls, tags, member);
		} catch (ServiceException e) {
			assert e.getMessage().contains("해당 큐레이션을 찾을 수 없습니다.");
		}
	}

	@Test
	@DisplayName("큐레이션을 삭제할 수 있다")
	void DeleteCuration() {
		// Mocking repository to return true for existence check
		when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));

		// Mocking the actual delete operation
		doNothing().when(curationRepository).deleteById(anyLong());
		doNothing().when(reportRepository).deleteByCurationId(anyLong());

		ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

		// Execute the service method to delete curation
		curationService.deleteCuration(1L, member);

		// Verify the delete operation was called once
		verify(curationRepository, times(1)).deleteById(1L);
	}

	@Test
	@DisplayName("실패 - 존재하지 않는 큐레이션을 삭제할 수 없다")
	void DeleteCurationNotFound() {
		// Mocking repository to return false for existence check
		when(curationRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Check if exception is thrown
		assertThatThrownBy(() -> curationService.deleteCuration(1L, member))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("해당 큐레이션을 찾을 수 없습니다.");

		// Verify that deleteById is never called because the curation does not exist
		verify(curationRepository, never()).deleteById(anyLong());
	}


	@Test
	@DisplayName("큐레이션을 조회할 수 있다")
	void GetCuration() {
		// HttpServletRequest 모킹
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.0.1");  // IP를 임의로 설정

		ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		SetOperations<String, Object> setOperations = mock(SetOperations.class);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);

		// Mocking repository to return a Curation
		when(curationRepository.findById(anyLong())).thenReturn(Optional.of(curation));

		when(memberService.isFollowed(any(), any())).thenReturn(true);

		Rq rq = mock(Rq.class);
		when(rq.isLogin()).thenReturn(true);
		when(rq.getActor()).thenReturn(mock(Member.class));
		ReflectionTestUtils.setField(curationService, "rq", rq);

		CurationDetailResDto retrievedCuration = curationService.getCuration(1L, request);

		// Verify the result
		assert retrievedCuration != null;
		assert retrievedCuration.getTitle().equals("Test Title");
	}

	@Test
	@DisplayName("큐레이션 조회수는 한 번만 증가해야 한다")
	void GetCurationMultipleTimes() {


		// HttpServletRequest 모킹
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.0.1");  // IP를 임의로 설정

		// Given: Redis와 큐레이션 관련 의존성 준비
		ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		SetOperations<String, Object> setOperations = mock(SetOperations.class);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);

		Rq rq = mock(Rq.class);
		when(rq.isLogin()).thenReturn(true);
		when(rq.getActor()).thenReturn(mock(Member.class));
		ReflectionTestUtils.setField(curationService, "rq", rq);

		doNothing().when(curationViewService).increaseViewCount(curation);

		// 첫 번째 조회에서만 true 반환하고, 그 이후에는 false 반환하도록 설정
		when(valueOperations.setIfAbsent(anyString(), eq("true"), eq(Duration.ofDays(1))))
				.thenReturn(true)  // 첫 번째 조회에서는 키가 없으므로 true 반환
				.thenReturn(false); // 두 번째 이후의 조회에서는 키가 이미 있으므로 false 반환

		// 큐레이션 조회 로직이 제대로 동작하도록 설정
		when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));

		// 조회수 초기 상태 저장
		Long initialViewCount = curation.getViewCount();

		// When: 큐레이션을 여러 번 조회한다
		curationService.getCuration(1L, request);  // 첫 번째 조회
		curationService.getCuration(1L, request);  // 두 번째 조회
		curationService.getCuration(1L, request);  // 세 번째 조회

		// Then: 조회수는 한 번만 증가해야 한다
		verify(curationViewService, times(1)).increaseViewCount(curation);
	}

	@Test
	@DisplayName("실패 - 존재하지 않는 큐레이션을 조회하면 실패한다")
	void GetCurationNotFound() {
		// HttpServletRequest 모킹
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.0.1");  // IP를 임의로 설정

		ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// Mocking repository to return empty Optional
		when(curationRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Check if exception is thrown
		try {
			curationService.getCuration(1L, request);
		} catch (ServiceException e) {
			assert e.getMessage().contains("해당 큐레이션을 찾을 수 없습니다.");
		}
	}

	@Test
	void findAllCuration() {

		SetOperations<String, Object> setOperations = mock(SetOperations.class);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);

		when(curationRepository.searchByFilters(ArgumentMatchers.anyList(), anyInt(), anyString(), anyString(), any(), any()))
			.thenReturn(new PageImpl<>(List.of(curation), PageRequest.of(0, 20), 1));

		List<CurationResDto> foundCurations = curationService.searchCurations(List.of("tag"), "title", "content", null,
			SearchOrder.LATEST,1,20).getCurations();

		// Verify the result
		assert foundCurations != null;
		assert foundCurations.size() == 1;
	}





	@Test
	@DisplayName("큐레이션 좋아요 기능을 테스트합니다.")
	void likeCuration() {
		Long curationId = 1L;
		Long memberId = 1L;

		String redisKey = "curation_like:" + curationId;
		String redisValue = String.valueOf(memberId);

		// 실제 큐레이션과 멤버 객체
		Curation mockCuration = mock(Curation.class);
		Member mockMember = mock(Member.class);

		// 레디스 LUA 실행 결과: 1이면 좋아요 추가, 0이면 삭제
		when(redisTemplate.execute(
				any(DefaultRedisScript.class),
				eq(Collections.singletonList(redisKey)),
				eq(redisValue)
		)).thenReturn(1L); // 좋아요 추가된 상황 가정

		// 저장소 모킹
		when(curationRepository.findById(curationId)).thenReturn(Optional.of(mockCuration));
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));

		// 실행
		curationService.likeCuration(curationId, memberId);

		// 검증
		verify(curationRepository, times(1)).findById(curationId);
		verify(memberRepository, times(1)).findById(memberId);
		verify(redisTemplate, times(1)).execute(
				any(DefaultRedisScript.class),
				eq(Collections.singletonList(redisKey)),
				eq(redisValue)
		);
	}




	@Test
	@DisplayName("큐레이션 좋아요를 한 번 더 누르면 Redis에서 취소 처리가 되어야 합니다.")
	void likeCurationWithCancel() {
		Long curationId = 1L;
		Long memberId = 1L;

		String redisKey = "curation_like:" + curationId;
		String redisValue = String.valueOf(memberId);

		// Redis에서 좋아요가 이미 있어서 제거됨
		when(redisTemplate.execute(
				any(DefaultRedisScript.class),
				eq(Collections.singletonList(redisKey)),
				eq(redisValue)
		)).thenReturn(0L);

		Curation mockCuration = mock(Curation.class);
		Member mockMember = mock(Member.class);

		when(curationRepository.findById(eq(curationId))).thenReturn(Optional.of(mockCuration));
		when(memberRepository.findById(eq(memberId))).thenReturn(Optional.of(mockMember));

		curationService.likeCuration(curationId, memberId);

		// 검증
		verify(redisTemplate, times(1)).execute(
				any(DefaultRedisScript.class),
				eq(Collections.singletonList(redisKey)),
				eq(redisValue)
		);

		verify(curationRepository, times(1)).findById(eq(curationId));
		verify(memberRepository, times(1)).findById(eq(memberId));

		// likeRepository는 호출되지 않아야 함
		verifyNoInteractions(likeRepository);
	}




	@Test
	@DisplayName("존재하지 않는 큐레이션에 좋아요를 누르면 예외가 발생해야 합니다.")
	void likeNonExistentCuration() {
		// Mocking repository to return empty Optional (큐레이션 없음)
		when(curationRepository.findById(anyLong())).thenReturn(Optional.empty());

		// 예외 발생 검증
		assertThatThrownBy(() -> curationService.likeCuration(1L, 1L))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("해당 큐레이션을 찾을 수 없습니다.");

		// Verify interactions (likeRepository는 호출되지 않아야 함)
		verify(curationRepository, times(1)).findById(anyLong());
		verify(memberRepository, never()).findById(anyLong());
		verify(likeRepository, never()).findByCurationAndMember(any(Curation.class), any(Member.class));
		verify(likeRepository, never()).save(any(Like.class));
	}

	@Test
	@DisplayName("존재하지 않는 멤버가 좋아요를 누르면 예외가 발생해야 합니다.")
	void likeByNonExistentMember() {
		// Mocking repository to return a valid Curation
		when(curationRepository.findById(anyLong())).thenReturn(Optional.of(curation));

		// Mocking repository to return empty Optional (멤버 없음)
		when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

		// 예외 발생 검증
		assertThatThrownBy(() -> curationService.likeCuration(1L, 1L))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("해당 멤버를 찾을 수 없습니다.");

		// Verify interactions (likeRepository는 호출되지 않아야 함)
		verify(curationRepository, times(1)).findById(anyLong());
		verify(memberRepository, times(1)).findById(anyLong());
		verify(likeRepository, never()).findByCurationAndMember(any(Curation.class), any(Member.class));
		verify(likeRepository, never()).save(any(Like.class));
	}

	@Test
	@DisplayName("큐레이션 좋아요를 Redis에서 DB로 동기화합니다.")
	void testSyncLikesToDatabase() {
		String key = "curation_like:1";
		Set<String> keys = Set.of(key);
		Set<String> memberIds = Set.of("100", "101");

		Curation curation = new Curation();

		SetOperations<String, Object> setOperations = mock(SetOperations.class);

		when(redisTemplate.keys("curation_like:*")).thenReturn(keys);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.members(key)).thenReturn(new HashSet<>(memberIds)); // ✅ 수정된 부분
		when(setOperations.size(key)).thenReturn((long) memberIds.size());

		when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));
		when(memberRepository.findByMemberId(anyString())).thenReturn(Optional.of(new Member()));

		curationService.syncLikesToDatabase();

		verify(likeRepository, times(2)).save(any(Like.class));
		verify(curationRepository, times(3)).findById(1L); // 1: 좋아요 저장용, 2: likeCount 업데이트용
		verify(curationRepository, times(1)).save(curation);
	}


	@Test
	@DisplayName("큐레이션에 대한 좋아요 여부를 Redis에서 확인합니다.")
	void testIsLikedByMember_ReturnsTrue() {
		Long curationId = 1L;
		Long memberId = 100L;
		String key = "curation_like:" + curationId;

		SetOperations<String, Object> setOperations = mock(SetOperations.class);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(redisTemplate.opsForSet().isMember(key, String.valueOf(memberId))).thenReturn(true);

		boolean result = curationService.isLikedByMember(curationId, memberId);

		assertTrue(result);
	}

	@Test
	@DisplayName("팔로잉한 유저의 큐레이션을 가져옵니다.")
	void testGetFollowingCurations() {

		List<Curation> mockCurations = List.of(curation);
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

		when(curationRepository.findFollowingCurations(eq(1L), any(Pageable.class))).thenReturn(mockCurations);

		List<CurationResDto> result = curationService.getFollowingCurations(member, 0, 10);

		assertEquals(1, result.size());
	}


	@Test
	@DisplayName("큐레이션을 신고합니다.")
	void testReportCuration() {
		Member actor = new Member();
		Curation curation = new Curation();

		Rq rq = mock(Rq.class);
		when(rq.getActor()).thenReturn(mock(Member.class));
		ReflectionTestUtils.setField(curationService, "rq", rq);

		when(rq.getActor()).thenReturn(actor);
		when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));

		curationService.reportCuration(1L, ReportType.SPAM);

		// 여기에 report 저장 등의 로직이 추가되었다면 검증해줘야 함
		verify(reportRepository, times(1)).save(any());
	}

	@Test
	@DisplayName("조회수가 가장 높은 3개의 큐레이션을 가져옵니다.")
	void testGetTrendingCuration_whenRedisHasData() {
		// Given

		Curation c1 = new Curation(
				"Test Title1",
				"Test Content1",
				member
		);
		c1.setViewCount(30);

		Curation c2 = new Curation(
				"Test Title2",
				"Test Content2",
				member
		);
		c2.setViewCount(40);
		Curation c3 = new Curation(
				"Test Title3",
				"Test Content3",
				member
		);
		c3.setViewCount(50);


		ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

		when(zSetOperations.reverseRange("day_view_count:", 0, 2)).thenReturn(Set.of("1", "2", "3"));

		when(curationRepository.findById(1L)).thenReturn(Optional.of(c1));
		when(curationRepository.findById(2L)).thenReturn(Optional.of(c2));
		when(curationRepository.findById(3L)).thenReturn(Optional.of(c3));

		when(zSetOperations.score("day_view_count:", "1")).thenReturn(50.0);
		when(zSetOperations.score("day_view_count:", "2")).thenReturn(40.0);
		when(zSetOperations.score("day_view_count:", "3")).thenReturn(30.0);

		// When
		TrendingCurationResDto result = curationService.getTrendingCuration();

		// Then
		assertNotNull(result);
		assertEquals(3, result.getCurations().size());
		assertEquals(50L, result.getCurations().get(0).getViewCount()); // 첫 번째 아이템의 조회수 검증
	}

	@Test
	@DisplayName("조회수가 가장 높은 3개의 큐레이션을 가져올때 Redis에 데이터가 없을 경우 DB에서 가져옵니다.")
	void testGetTrendingCuration_whenRedisIsEmpty_thenFallbackToDb() {
		// Given
		ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(zSetOperations.reverseRange("day_view_count:", 0, 2)).thenReturn(Collections.emptySet());

		Curation c1 = new Curation(
				"Test Title1",
				"Test Content1",
				member
		);
		Curation c2 = new Curation(
				"Test Title2",
				"Test Content2",
				member
		);
		Curation c3 = new Curation(
				"Test Title1",
				"Test Content1",
				member
		);

		when(curationRepository.findTop3ByOrderByViewCountDesc()).thenReturn(List.of(c1, c2, c3));

		// When
		TrendingCurationResDto result = curationService.getTrendingCuration();

		// Then
		assertNotNull(result);
		assertEquals(3, result.getCurations().size());
		assertEquals(100, result.getCurations().get(0).getViewCount());
	}
}