package com.team8.project2.domain.comment.controller

import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.comment.repository.CommentRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
class ApiV1GlobalCommentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val author = memberRepository.findById(2L)
            .orElseThrow { RuntimeException("BaseInitData: loginId=2 not found") }

        accessToken = authTokenService.genAccessToken(author)
    }

    @Test
    @DisplayName("BaseInitData 기반 - 댓글이 있는 사용자(other)가 자신의 댓글을 삭제할 수 있다")
    fun deleteCommentOfOther() {
        val myComment = commentRepository.findAll()
            .firstOrNull { it.author.id == 2L }
            ?: throw RuntimeException("BaseInitData: loginId=2의 댓글 없음")

        mockMvc.perform(
            delete("/api/v1/comments/{id}", myComment.id)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("댓글이 삭제되었습니다."))

        assertThat(commentRepository.findById(myComment.id)).isEmpty
    }

    @Test
    @DisplayName("BaseInitData 기반 - 내 댓글 조회")
    fun getMyCommentsList() {
        val member1 = memberRepository.findById(1L)
            .orElseThrow { RuntimeException("BaseInitData: loginId=1 not found") }
        val tokenForUser1 = authTokenService.genAccessToken(member1)

        mockMvc.perform(
            get("/api/v1/comments/mycomments")
                .header("Authorization", "Bearer $tokenForUser1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("내 댓글 조회 성공"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0)) // loginId=1은 댓글 작성 X
    }
}
