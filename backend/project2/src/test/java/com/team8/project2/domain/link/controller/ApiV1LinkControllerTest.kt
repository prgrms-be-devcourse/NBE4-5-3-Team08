package com.team8.project2.domain.link.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.repository.LinkRepository
import com.team8.project2.domain.link.service.LinkClickService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional


@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiV1LinkControllerTest.TestConfig::class)
class ApiV1LinkControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    @Autowired
    private lateinit var linkClickService: LinkClickService

    private lateinit var author: Member
    private lateinit var accessToken: String

    @TestConfiguration
    class TestConfig {
        @Bean
        fun linkClickService(): LinkClickService = Mockito.mock(LinkClickService::class.java)
    }

    @BeforeEach
    fun setUp() {
        author = memberRepository.findById(1L).orElseThrow()
        accessToken = authTokenService.genAccessToken(author)
    }

    @Test
    @DisplayName("링크 등록이 성공한다")
    fun addLink() {
        val dto = LinkReqDTO(
            url = "https://test.com",
            title = null,
            description = null
        )

        mockMvc.perform(
            post("/api/v1/link")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("링크가 성공적으로 추가되었습니다."))
            .andExpect(jsonPath("$.data.url").value("https://test.com"))
    }

    @Test
    @DisplayName("링크를 수정할 수 있다")
    fun updateLink() {
        val saved = linkRepository.save(
            Link.builder()
                .url("https://original.com")
                .click(0)
                .build()
        )

        val updateDto = LinkReqDTO(
            url = "https://updated.com",
            title = null,
            description = null
        )

        mockMvc.perform(
            put("/api/v1/link/{linkId}", saved.id)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("링크가 성공적으로 수정되었습니다."))
            .andExpect(jsonPath("$.data.url").value("https://updated.com"))
    }

    @Test
    @DisplayName("링크를 삭제할 수 있다")
    fun deleteLink() {
        val link = linkRepository.save(
            Link.builder()
                .url("https://to-be-deleted.com")
                .click(0)
                .build()
        )

        mockMvc.perform(
            delete("/api/v1/link/{linkId}", link.id)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$.code").value("204-1"))
            .andExpect(jsonPath("$.msg").value("링크가 성공적으로 삭제되었습니다."))

        assertThat(linkRepository.findById(link.id!!)).isEmpty()
    }

    @Test
    @DisplayName("링크 조회 시 클릭 수가 증가한다")
    fun getLinkAndIncrementClick() {
        val saved = linkRepository.save(
            Link.builder()
                .url("https://click.com")
                .click(0)
                .build()
        )

        doNothing().`when`(linkClickService).increaseClickCount(any<Link>())

        mockMvc.perform(
            get("/api/v1/link/{linkId}", saved.id)
                .header("X-FORWARDED-FOR", "123.123.123.123")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("링크가 성공적으로 조회되었습니다."))
            .andExpect(jsonPath("$.data.url").value("https://click.com"))
    }

}
