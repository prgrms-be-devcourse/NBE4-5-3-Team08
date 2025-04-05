package com.team8.project2.domain.curation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.project2.domain.curation.curation.dto.CurationReqDTO;
import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.curation.repository.CurationRepository;
import com.team8.project2.domain.curation.curation.service.CurationService;
import com.team8.project2.domain.curation.tag.dto.TagReqDto;
import com.team8.project2.domain.link.dto.LinkReqDTO;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.entity.RoleEnum;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.member.service.AuthTokenService;
import com.team8.project2.global.RedisUtils;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class ApiV1CurationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurationService curationService;

	@Autowired
	private CurationRepository curationRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private RedisUtils redisUtils;

	private CurationReqDTO curationReqDTO;

	String memberAccessKey;
	Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.findById(1L).get();
		memberAccessKey = authTokenService.genAccessToken(member);

		// CurationReqDTO 설정 (링크 포함)
		curationReqDTO = new CurationReqDTO();
		curationReqDTO.setTitle("Test Title");
		curationReqDTO.setContent("Test Content");

		// LinkReqDTO 생성
		LinkReqDTO linkReqDTO = new LinkReqDTO();
		linkReqDTO.setUrl("https://example.com");

		// TagReqDTO
		TagReqDto tagReqDto = new TagReqDto();
		tagReqDto.setName("test");

		// 링크 리스트에 추가
		curationReqDTO.setLinkReqDtos(Collections.singletonList(linkReqDTO));
		// 태그 리스트에 추가
		curationReqDTO.setTagReqDtos(Collections.singletonList(tagReqDto));

		// Redis 데이터 초기화
		redisUtils.clearAllData();
	}

	@Test
	@DisplayName("큐레이션을 생성할 수 있다")
	void createCuration() throws Exception {
		mockMvc.perform(post("/api/v1/curation")
				.header("Authorization", "Bearer " + memberAccessKey)
				.contentType("application/json")
				.content(new ObjectMapper().writeValueAsString(curationReqDTO)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("201-1"))
			.andExpect(jsonPath("$.msg").value("글이 성공적으로 생성되었습니다."))
			.andExpect(jsonPath("$.data.title").value("Test Title"))
			.andExpect(jsonPath("$.data.content").value("Test Content"))
			.andExpect(jsonPath("$.data.urls[0].url").value("https://example.com"))
			.andExpect(jsonPath("$.data.tags[0].name").value("test"));
	}

	@Test
	@DisplayName("큐레이션을 수정할 수 있다")
	void updateCuration() throws Exception {
		Curation savedCuration = curationRepository.findById(1L).orElseThrow();

		// 수정된 curationReqDTO를 사용하여 PUT 요청
		mockMvc.perform(put("/api/v1/curation/{id}", savedCuration.getId()).contentType("application/json")
				.header("Authorization", "Bearer " + memberAccessKey) // JWT 포함 요청
				.content(new ObjectMapper().writeValueAsString(curationReqDTO)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 성공적으로 수정되었습니다."))
			.andExpect(jsonPath("$.data.title").value("Test Title"))
			.andExpect(jsonPath("$.data.content").value("Test Content"))
			.andExpect(jsonPath("$.data.urls.length()").value(1))
			.andExpect(jsonPath("$.data.urls[0].url").value("https://example.com"))
			.andExpect(jsonPath("$.data.tags.length()").value(1))
			.andExpect(jsonPath("$.data.tags[0].name").value("test"));
	}

	@Test
	@DisplayName("실패 - 작성자가 아니면 큐레이션 수정에 실패한다")
	void updateCurationByOtherUser_ShouldFail() throws Exception {
		// 다른 사용자 생성
		Member anotherMember = Member.builder()
			.memberId("otherperson")
			.username("otherperson")
			.password("otherperson")
			.email("other@example.com")
			.role(RoleEnum.MEMBER)
			.introduce("otherperson")
			.build();
		memberRepository.save(anotherMember);

		Curation savedCuration = curationRepository.findById(1L).orElseThrow();

		// 다른 사용자의 인증 토큰 생성
		String otherAccessToken = authTokenService.genAccessToken(anotherMember);

		mockMvc.perform(put("/api/v1/curation/{id}", savedCuration.getId())
				.contentType("application/json")
				.header("Authorization", "Bearer " + otherAccessToken)
				.content(new ObjectMapper().writeValueAsString(curationReqDTO)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("큐레이션 작성자는 큐레이션을 삭제할 수 있다")
	void deleteCuration() throws Exception {
		Curation savedCuration = curationRepository.findById(1L).orElseThrow();

		// Member 인증 설정 후 삭제 요청
		mockMvc.perform(delete("/api/v1/curation/{id}", savedCuration.getId())
				.header("Authorization", "Bearer " + memberAccessKey))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("큐레이션을 조회할 수 있다")
	void getCuration() throws Exception {
		mockMvc.perform(get("/api/v1/curation/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("조회 성공"))
			.andExpect(jsonPath("$.data.title").value("최신 개발 트렌드"))
			.andExpect(jsonPath("$.data.urls[0].url").value("https://www.naver.com/"))
			.andExpect(jsonPath("$.data.urls[1].url").value("https://www.github.com/"))
			.andExpect(jsonPath("$.data.tags[0].name").value("개발"))
			.andExpect(jsonPath("$.data.tags[1].name").value("프로그래밍"))
			.andExpect(jsonPath("$.data.comments[0].content").value("더 많은 예시가 있으면 좋겠어요."));
	}

	@Test
	@DisplayName("큐레이션을 전체 조회할 수 있다")
	void findAll() throws Exception {
		for (int i = 0; i < 10; i++) {
			curationService.createCuration(curationReqDTO.getTitle(), curationReqDTO.getContent(),
				curationReqDTO.getLinkReqDtos()
					.stream()
					.map(linkReqDto -> linkReqDto.getUrl())
					.collect(Collectors.toUnmodifiableList()), curationReqDTO.getTagReqDtos()
					.stream()
					.map(tagReqDto -> tagReqDto.getName())
					.collect(Collectors.toUnmodifiableList()), member);
		}

		mockMvc.perform(get("/api/v1/curation"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(20))
			.andExpect(jsonPath("$.data.totalPages").value(11))
			.andExpect(jsonPath("$.data.totalElements").value(210))
			.andExpect(jsonPath("$.data.numberOfElements").value(20))
			.andExpect(jsonPath("$.data.size").value(20));
	}

	@Test
	@DisplayName("큐레이션을 태그로 검색할 수 있다")
	void findCurationByTags() throws Exception {
		createCurationWithTags(List.of("ex1", "ex2", "ex3"));
		createCurationWithTags(List.of("ex2", "ex3", "ex4", "ex5"));
		createCurationWithTags(List.of("ex2", "ex1", "ex3"));
		mockMvc.perform(get("/api/v1/curation").param("tags", "ex1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(2));
	}

	private Curation createCurationWithTags(List<String> tags) {
		return curationService.createCuration(curationReqDTO.getTitle(), curationReqDTO.getContent(),
			curationReqDTO.getLinkReqDtos()
				.stream()
				.map(linkReqDto -> linkReqDto.getUrl())
				.collect(Collectors.toUnmodifiableList()), tags, member);
	}

	@Test
	@DisplayName("큐레이션을 제목으로 검색할 수 있다")
	void findCurationByTitle() throws Exception {
		createCurationWithTitle("ex1");
		createCurationWithTitle("test-ex");
		createCurationWithTitle("test");

		mockMvc.perform(get("/api/v1/curation").param("title", "ex"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(2));
	}

	private Curation createCurationWithTitle(String title) {
		return curationService.createCuration(title, curationReqDTO.getContent(), curationReqDTO.getLinkReqDtos()
			.stream()
			.map(linkReqDto -> linkReqDto.getUrl())
			.collect(Collectors.toUnmodifiableList()), curationReqDTO.getTagReqDtos()
			.stream()
			.map(tagReqDto -> tagReqDto.getName())
			.collect(Collectors.toUnmodifiableList()), member);
	}

	@Test
	@DisplayName("내용으로 큐레이션을 검색할 수 있다")
	void findCurationByContent() throws Exception {
		createCurationWithContent("example");
		createCurationWithContent("test-example");
		createCurationWithContent("test");

		mockMvc.perform(get("/api/v1/curation").param("content", "example"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.length()").value(5));
	}

	private Curation createCurationWithContent(String content) {
		return curationService.createCuration(curationReqDTO.getTitle(), content, curationReqDTO.getLinkReqDtos()
			.stream()
			.map(linkReqDto -> linkReqDto.getUrl())
			.collect(Collectors.toUnmodifiableList()), curationReqDTO.getTagReqDtos()
			.stream()
			.map(tagReqDto -> tagReqDto.getName())
			.collect(Collectors.toUnmodifiableList()), member);
	}

	@Test
	@DisplayName("제목과 내용으로 큐레이션을 검색할 수 있다")
	void findCurationByTitleAndContent() throws Exception {
		createCurationWithTitleAndContent("popular", "famous1");
		createCurationWithTitleAndContent("sample", "test-famous");
		createCurationWithTitleAndContent("test", "test");

		mockMvc.perform(get("/api/v1/curation").param("title", "popular").param("content", "famous"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.length()").value(5));
	}

	private Curation createCurationWithTitleAndContent(String title, String content) {
		return curationService.createCuration(title, content, curationReqDTO.getLinkReqDtos()
			.stream()
			.map(linkReqDto -> linkReqDto.getUrl())
			.collect(Collectors.toUnmodifiableList()), curationReqDTO.getTagReqDtos()
			.stream()
			.map(tagReqDto -> tagReqDto.getName())
			.collect(Collectors.toUnmodifiableList()), member);
	}

	@Test
	@DisplayName("최신순으로 큐레이션을 전체 조회할 수 있다")
	void findCurationByLatest() throws Exception {
		createCurationWithTitleAndContent("title1", "content1");
		createCurationWithTitleAndContent("title2", "content2");
		createCurationWithTitleAndContent("title3", "content3");

		mockMvc.perform(get("/api/v1/curation").param("order", "LATEST"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations[0].content").value("content3"))
			.andExpect(jsonPath("$.data.curations[1].content").value("content2"))
			.andExpect(jsonPath("$.data.curations[2].content").value("content1"));
	}

	@Test
	@DisplayName("오래된 순으로 큐레이션을 전체 조회할 수 있다")
	void findCurationByOldest() throws Exception {
		mockMvc.perform(get("/api/v1/curation").param("order", "OLDEST"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(20))
			.andExpect(jsonPath("$.data.curations[0].id").value(1))
			.andExpect(jsonPath("$.data.curations[1].id").value(2))
			.andExpect(jsonPath("$.data.curations[2].id").value(3))
			.andExpect(jsonPath("$.data.curations[3].id").value(4))
			.andExpect(jsonPath("$.data.curations[4].id").value(5))
			.andExpect(jsonPath("$.data.totalPages").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(200))
			.andExpect(jsonPath("$.data.numberOfElements").value(20))
			.andExpect(jsonPath("$.data.size").value(20));
	}

	@Test
	@DisplayName("좋아요 순으로 큐레이션을 전체 조회할 수 있다")
	void findCurationByLikeCount() throws Exception {
		createCurationWithTitleAndContentAndLikeCount("title1", "content1", 4L);
		createCurationWithTitleAndContentAndLikeCount("title2", "content2", 10L);
		createCurationWithTitleAndContentAndLikeCount("title3", "content3", 2L);

		mockMvc.perform(get("/api/v1/curation").param("order", "LIKECOUNT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations[0].content").value("content2"))
			.andExpect(jsonPath("$.data.curations[1].content").value("content1"))
			.andExpect(jsonPath("$.data.curations[2].content").value("content3"));
	}

	private Curation createCurationWithTitleAndContentAndLikeCount(String title, String content, Long likeCount) {
		Curation curation = curationService.createCuration(title, content, curationReqDTO.getLinkReqDtos()
				.stream()
				.map(linkReqDto -> linkReqDto.getUrl())
				.collect(Collectors.toList()),
			curationReqDTO.getTagReqDtos().stream().map(tagReqDto -> tagReqDto.getName()).collect(Collectors.toList()),
			member);

		curation.setLikeCount(likeCount);
		curationRepository.save(curation);
		return curation;
	}

	@Test
	@DisplayName("큐레이션에 좋아요를 할 수 있다")
	void likeCuration() throws Exception {
		Curation savedCuration = curationService.createCuration("Test Title", "Test Content",
			curationReqDTO.getLinkReqDtos()
				.stream()
				.map(linkReqDto -> linkReqDto.getUrl())
				.collect(Collectors.toUnmodifiableList()), curationReqDTO.getTagReqDtos()
				.stream()
				.map(tagReqDto -> tagReqDto.getName())
				.collect(Collectors.toUnmodifiableList()), member);

		mockMvc.perform(
				post("/api/v1/curation/like/{id}", savedCuration.getId())
					.header("Authorization", "Bearer " + memberAccessKey)
					.param("memberId", String.valueOf(member.getMemberId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글에 좋아요를 했습니다."))
			.andExpect(jsonPath("$.data").doesNotExist()); // 응답 데이터가 없음을 확인
	}

	@Test
	@DisplayName("큐레이션 작성자로 큐레이션을 검색할 수 있다")
	void findCurationByAuthor() throws Exception {
		var author1 = createMember("author1");
		var author2 = createMember("author2");

		createCurationWithTitleAndMember("title1", author1);
		createCurationWithTitleAndMember("title2", author2);
		createCurationWithTitleAndMember("title3", author1);

		mockMvc.perform(get("/api/v1/curation").param("author", "author1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글이 검색되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(2));
	}

	private Member createMember(String author) {
		Member member = Member.builder()
			.email(author + "@gmail.com")
			.role(RoleEnum.MEMBER)
			.memberId(author)
			.username(author)
			.password("password")
			.profileImage("http://localhost:8080/images/team8-logo.png")
			.build();

		return memberRepository.save(member);
	}

	private void createCurationWithTitleAndMember(String title, Member author) {
		Curation curation = curationService.createCuration(title, "example content", List.of("https://www.google.com/"),
			List.of("tag1", "tag2"), member);
		curation.setMember(author);
		curationRepository.save(curation);
	}

	@Test
	@DisplayName("팔로우중인 큐레이터의 큐레이션을 전체 조회할 수 있다")
	void followingCuration() throws Exception {
		Member member = memberRepository.findById(3L).get();
		String accessToken = authTokenService.genAccessToken(member);

		mockMvc.perform(get("/api/v1/curation/following")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("팔로우중인 큐레이터의 큐레이션이 조회되었습니다."))
			.andExpect(jsonPath("$.data[0].id").value(178))
			.andExpect(jsonPath("$.data[1].id").value(177))
			.andExpect(jsonPath("$.data[2].id").value(134));
	}

	@Test
	@DisplayName("실패 - 인증 정보가 없으면 팔로우중인 큐레이션 조회에 실패한다")
	void followingCuration_noAuth() throws Exception {
		mockMvc.perform(get("/api/v1/curation/following"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-1"))
			.andExpect(jsonPath("$.msg").value("접근이 거부되었습니다. 로그인 상태를 확인해 주세요."));
	}

	@Test
	@DisplayName("특정 큐레이션을 포함하고 있는 자신의 플레이리스트 목록을 조회할 수 있다")
	void findPlaylistByCuration() throws Exception {
		var curationId = 1L;
		mockMvc.perform(get("/api/v1/curation/%d/playlists".formatted(curationId))
				.header("Authorization", "Bearer " + memberAccessKey))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("플레이리스트 조회 성공"))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].id").value(1));
	}

	@Test
	@DisplayName("트렌딩 태그를 조회할 수 있다")
	void getTrendingTag() throws Exception {
		mockMvc.perform(get("/api/v1/curation/trending-tag"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("트렌딩 태그가 조회되었습니다."))
			.andExpect(jsonPath("$.data.tags.length()").value(5));
	}

	@Test
	@DisplayName("트렌딩 큐레이션을 조회할 수 있다")
	void getTrendingCuration() throws Exception {
		mockMvc.perform(get("/api/v1/curation/trending-curation"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("트렌딩 큐레이션이 조회되었습니다."))
			.andExpect(jsonPath("$.data.curations.length()").value(3));
	}

	@Test
	@DisplayName("특정 큐레이션에 대해 자신이 좋아요를 눌렀는지 확인할 수 있다")
	void getLikeStatus() throws Exception {
		var curationId = 1L;
		// member1이 curation1에 대한 좋아요 여부 조회 - false
		mockMvc.perform(get("/api/v1/curation/like/%d/status".formatted(curationId))
				.header("Authorization", "Bearer " + memberAccessKey))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("좋아요 여부 확인 성공"))
			.andExpect(jsonPath("$.data").value(false));

		// member1이 curation1를 좋아요함
		mockMvc.perform(
				post("/api/v1/curation/like/{id}", curationId)
					.header("Authorization", "Bearer " + memberAccessKey)
					.param("memberId", String.valueOf(member.getMemberId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("글에 좋아요를 했습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		// member1이 curation1에 대한 좋아요 여부 조회 - true
		mockMvc.perform(get("/api/v1/curation/like/%d/status".formatted(curationId))
				.header("Authorization", "Bearer " + memberAccessKey))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-1"))
			.andExpect(jsonPath("$.msg").value("좋아요 여부 확인 성공"))
			.andExpect(jsonPath("$.data").value(true));
	}
}
