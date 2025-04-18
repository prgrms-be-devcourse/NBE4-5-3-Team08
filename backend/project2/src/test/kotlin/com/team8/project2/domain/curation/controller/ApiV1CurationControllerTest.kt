package com.team8.project2.domain.curation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team8.project2.domain.curation.curation.dto.CurationReqDTO
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.tag.dto.TagReqDto
import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import com.team8.project2.global.RedisUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiV1CurationControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var curationService: CurationService

    @Autowired
    lateinit var curationRepository: CurationRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var authTokenService: AuthTokenService

    @Autowired
    lateinit var redisUtils: RedisUtils

    lateinit var curationReqDTO: CurationReqDTO

    lateinit var memberAccessKey: String
    lateinit var member: Member

    @BeforeEach
    fun setUp() {
        member = memberRepository.findById(1L).get()
        memberAccessKey = authTokenService.genAccessToken(member)


        // LinkReqDTO 생성
        val linkReqDTO = LinkReqDTO("https://test.com", null, null)

        // TagReqDTO
        val tagReqDto = TagReqDto("test")

        // CurationReqDTO 설정 (링크 포함)
        curationReqDTO = CurationReqDTO(
            "Test Title", "Test Content",
            listOf(linkReqDTO), listOf(tagReqDto)
        )


        // Redis 데이터 초기화
        redisUtils.clearAllData()
    }

    @Test
    @DisplayName("큐레이션을 생성할 수 있다")
    @Throws(Exception::class)
    fun createCuration() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/curation")
                .header("Authorization", "Bearer $memberAccessKey")
                .contentType("application/json")
                .content(ObjectMapper().writeValueAsString(curationReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 성공적으로 생성되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("Test Title"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("Test Content"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.urls[0].url").value("https://test.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags[0].name").value("test"))
    }

    @Test
    @DisplayName("큐레이션을 수정할 수 있다")
    @Throws(Exception::class)
    fun updateCuration() {
        val savedCuration = curationRepository.findById(1L).orElseThrow()

        // 수정된 curationReqDTO를 사용하여 PUT 요청
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/curation/{id}", savedCuration.id).contentType("application/json")
                .header("Authorization", "Bearer $memberAccessKey") // JWT 포함 요청
                .content(ObjectMapper().writeValueAsString(curationReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 성공적으로 수정되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("Test Title"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("Test Content"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.urls.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.urls[0].url").value("https://test.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags[0].name").value("test"))
    }

    @Test
    @DisplayName("실패 - 작성자가 아니면 큐레이션 수정에 실패한다")
    @Throws(Exception::class)
    fun updateCurationByOtherUser_ShouldFail() {
        // 다른 사용자 생성
        val anotherMember = Member(
            loginId = "otherperson",
            username = "otherperson",
            password = "otherperson",
            role = RoleEnum.MEMBER,
            profileImage = null,
            email = "other@example.com",
            introduce = "otherperson"
        )
        memberRepository.save(anotherMember)

        val savedCuration = curationRepository.findById(1L).orElseThrow()

        // 다른 사용자의 인증 토큰 생성
        val otherAccessToken = authTokenService.genAccessToken(anotherMember)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/curation/{id}", savedCuration.id)
                .contentType("application/json")
                .header("Authorization", "Bearer $otherAccessToken")
                .content(ObjectMapper().writeValueAsString(curationReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @DisplayName("큐레이션 작성자는 큐레이션을 삭제할 수 있다")
    @Throws(Exception::class)
    fun deleteCuration() {
        val savedCuration = curationRepository.findById(1L).orElseThrow()

        // Member 인증 설정 후 삭제 요청
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/curation/{id}", savedCuration.id)
                .header("Authorization", "Bearer $memberAccessKey")
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())
    }

    @Test
    @DisplayName("큐레이션을 조회할 수 있다")
    @Throws(Exception::class)
    fun tgetCuration() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation/{id}", 1L))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("최신 개발 트렌드"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.urls[0].url").value("https://www.naver.com/"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.urls[1].url").value("https://www.github.com/"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags[0].name").value("개발"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags[1].name").value("프로그래밍"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.comments[0].content").value("정말 유용한 정보네요! 감사합니다."))
    }

    @Test
    @DisplayName("큐레이션을 전체 조회할 수 있다")
    @Throws(Exception::class)
    fun findAll() {
        for (i in 0..9) {
            curationService.createCuration(
                curationReqDTO.title,
                curationReqDTO.content,
                curationReqDTO.linkReqDtos!!.map { it.url },
                curationReqDTO.tagReqDtos!!.map { it.name },
                member
            )
        }

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalPages").value(11))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(210))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.numberOfElements").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value(20))
    }

    @Test
    @DisplayName("큐레이션을 태그로 검색할 수 있다")
    @Throws(Exception::class)
    fun findCurationByTags() {
        createCurationWithTags(listOf("ex1", "ex2", "ex3"))
        createCurationWithTags(listOf("ex2", "ex3", "ex4", "ex5"))
        createCurationWithTags(listOf("ex2", "ex1", "ex3"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("tags", "ex1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(2))
    }

    private fun createCurationWithTags(tags: List<String>): Curation {
        return curationService.createCuration(
            curationReqDTO.title,
            curationReqDTO.content,
            curationReqDTO.linkReqDtos!!.map { it.url },
            tags,
            member
        )
    }


    @Test
    @DisplayName("큐레이션을 제목으로 검색할 수 있다")
    @Throws(Exception::class)
    fun findCurationByTitle() {
        createCurationWithTitle("ex1")
        createCurationWithTitle("test-ex")
        createCurationWithTitle("test")

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("title", "ex"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(2))
    }

    private fun createCurationWithTitle(title: String): Curation {
        return curationService.createCuration(
            title,
            curationReqDTO.content,
            curationReqDTO.linkReqDtos!!.map { it.url },
            curationReqDTO.tagReqDtos!!.map { it.name },
            member
        )
    }


    @Test
    @DisplayName("내용으로 큐레이션을 검색할 수 있다")
    @Throws(Exception::class)
    fun findCurationByContent() {
        createCurationWithContent("example")
        createCurationWithContent("test-example")
        createCurationWithContent("test")

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("content", "example"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(5))
    }

    private fun createCurationWithContent(content: String): Curation {
        val linkUrls = curationReqDTO.linkReqDtos!!.map { it.url }
        val tagNames = curationReqDTO.tagReqDtos!!.map { it.name }

        return curationService.createCuration(
            curationReqDTO.title,
            content,
            linkUrls,
            tagNames,
            member
        )
    }


    @Test
    @DisplayName("제목과 내용으로 큐레이션을 검색할 수 있다")
    @Throws(Exception::class)
    fun findCurationByTitleAndContent() {
        createCurationWithTitleAndContent("popular", "famous1")
        createCurationWithTitleAndContent("sample", "test-famous")
        createCurationWithTitleAndContent("test", "test")

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/curation").param("title", "popular").param("content", "famous")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(5))
    }

    private fun createCurationWithTitleAndContent(title: String, content: String): Curation {
        return curationService.createCuration(
            title,
            content,
            curationReqDTO.linkReqDtos!!.map { it.url },
            curationReqDTO.tagReqDtos!!.map { it.name },
            member
        )
    }


    @Test
    @DisplayName("최신순으로 큐레이션을 전체 조회할 수 있다")
    @Throws(Exception::class)
    fun findCurationByLatest() {
        createCurationWithTitleAndContent("title1", "content1")
        createCurationWithTitleAndContent("title2", "content2")
        createCurationWithTitleAndContent("title3", "content3")

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("order", "LATEST"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[0].content").value("content3"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[1].content").value("content2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[2].content").value("content1"))
    }

    @Test
    @DisplayName("오래된 순으로 큐레이션을 전체 조회할 수 있다")
    @Throws(Exception::class)
    fun findCurationByOldest() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("order", "OLDEST"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[0].id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[1].id").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[2].id").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[3].id").value(4))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[4].id").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalPages").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.numberOfElements").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value(20))
    }

    @Test
    @DisplayName("좋아요 순으로 큐레이션을 전체 조회할 수 있다")
    @Throws(Exception::class)
    fun findCurationByLikeCount() {
        createCurationWithTitleAndContentAndLikeCount("title1", "content1", 4L)
        createCurationWithTitleAndContentAndLikeCount("title2", "content2", 10L)
        createCurationWithTitleAndContentAndLikeCount("title3", "content3", 2L)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("order", "LIKECOUNT"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[0].content").value("content2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[1].content").value("content1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations[2].content").value("content3"))
    }

    private fun createCurationWithTitleAndContentAndLikeCount(
        title: String,
        content: String,
        likeCount: Long
    ): Curation {
        val linkUrls = curationReqDTO.linkReqDtos!!.map { it.url }
        val tagNames = curationReqDTO.tagReqDtos!!.map { it.name }

        val curation = curationService.createCuration(
            title,
            content,
            linkUrls,
            tagNames,
            member
        )

        curation.likeCount = likeCount
        curationRepository.save(curation)
        return curation
    }

    @Test
    @DisplayName("큐레이션에 좋아요를 할 수 있다")
    @Throws(Exception::class)
    fun likeCuration() {
        val savedCuration = curationService.createCuration(
            "Test Title", "Test Content",
            curationReqDTO.linkReqDtos!!.map { it.url },
            curationReqDTO.tagReqDtos!!.map { it.name },
            member
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/curation/like/{id}", savedCuration.id)
                .header("Authorization", "Bearer $memberAccessKey")
                .param("loginId", member.getLoginId())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글에 좋아요를 했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }


    @Test
    @DisplayName("큐레이션 작성자로 큐레이션을 검색할 수 있다")
    @Throws(Exception::class)
    fun findCurationByAuthor() {
        val author1 = createMember("author1")
        val author2 = createMember("author2")

        createCurationWithTitleAndMember("title1", author1)
        createCurationWithTitleAndMember("title2", author2)
        createCurationWithTitleAndMember("title3", author1)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation").param("author", "author1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글이 검색되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(2))
    }

    private fun createMember(author: String): Member {
        val member =  Member(
            loginId = author,
            username = author,
            password = "password",
            role = RoleEnum.MEMBER,
            profileImage = "http://localhost:8080/images/team8-logo.png",
            email = "$author@gmail.com",
            introduce = null
        )

        return memberRepository.save(member)
    }

    private fun createCurationWithTitleAndMember(title: String, author: Member) {
        val curation = curationService.createCuration(
            title, "example content", listOf("https://www.google.com/"),
            listOf("tag1", "tag2"), author
        )
        curationRepository.save(curation)
    }

    @Test
    @DisplayName("팔로우중인 큐레이터의 큐레이션을 전체 조회할 수 있다")
    @Throws(Exception::class)
    fun followingCuration() {
        val member = memberRepository.findById(3L).get()
        val accessToken = authTokenService.genAccessToken(member)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/curation/following")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("팔로우중인 큐레이터의 큐레이션이 조회되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(178))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(177))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[2].id").value(134))
    }

    @Test
    @DisplayName("실패 - 인증 정보가 없으면 팔로우중인 큐레이션 조회에 실패한다")
    @Throws(
        Exception::class
    )
    fun followingCuration_noAuth() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation/following"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("접근이 거부되었습니다. 로그인 상태를 확인해 주세요."))
    }

    @Test
    @DisplayName("특정 큐레이션을 포함하고 있는 자신의 플레이리스트 목록을 조회할 수 있다")
    @Throws(
        Exception::class
    )
    fun findPlaylistByCuration() {
        val curationId = 1L
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/curation/${curationId}/playlists")
                .header("Authorization", "Bearer $memberAccessKey")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("플레이리스트 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1))
    }

    @Test
    @DisplayName("트렌딩 태그를 조회할 수 있다")
    @Throws(Exception::class)
    fun tgetTrendingTag() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation/trending-tag"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("트렌딩 태그가 조회되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tags.length()").value(5))
    }

    @Test
    @DisplayName("트렌딩 큐레이션을 조회할 수 있다")
    @Throws(Exception::class)
    fun tgetTrendingCuration() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/curation/trending-curation"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("트렌딩 큐레이션이 조회되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.curations.length()").value(3))
    }

    @Test
    @DisplayName("특정 큐레이션에 대해 자신이 좋아요를 눌렀는지 확인할 수 있다")
    @Throws(
        Exception::class
    )
    fun tgetLikeStatus() {
        val curationId = 1L
        // member1이 curation1에 대한 좋아요 여부 조회 - false
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/curation/like/${curationId}/status")
                .header("Authorization", "Bearer $memberAccessKey")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요 여부 확인 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(false))

        // member1이 curation1를 좋아요함
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/curation/like/{id}", curationId)
                .header("Authorization", "Bearer $memberAccessKey")
                .param("loginId", member.getLoginId())
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("글에 좋아요를 했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())

        // member1이 curation1에 대한 좋아요 여부 조회 - true
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/curation/like/${curationId}/status")
                .header("Authorization", "Bearer $memberAccessKey")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("좋아요 여부 확인 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
    }
}
