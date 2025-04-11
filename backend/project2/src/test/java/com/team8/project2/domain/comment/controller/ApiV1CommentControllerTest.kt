package com.team8.project2.domain.comment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import com.team8.project2.domain.comment.dto.CommentDto
import com.team8.project2.domain.comment.repository.CommentRepository
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import com.team8.project2.domain.member.service.MemberService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiV1CommentControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var commentService: CommentService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var authTokenService: AuthTokenService

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Autowired
    lateinit var memberService: MemberService

    lateinit var authorAccessKey: String
    lateinit var author: Member

    @BeforeEach
    fun setUp() {
        author = memberRepository!!.findById(1L).get()
        authorAccessKey = authTokenService!!.genAccessToken(author)
    }

    @Test
    @DisplayName("BaseInitData 기반 - 댓글을 작성할 수 있다")
    fun createComment() {
        val commentDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "BaseInitData 댓글 작성 테스트")

        mockMvc.perform(
            post("/api/v1/curations/1/comments")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(commentDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.data.authorName").value("username"))
            .andExpect(jsonPath("$.data.content").value("BaseInitData 댓글 작성 테스트"))

        assertThat(commentService.getCommentsByCurationId(1L)).hasSize(4)
    }

    @Test
    @DisplayName("실패 - 인증 정보가 없으면 댓글 작성에 실패한다")
    fun createCommentWithNoAuth() {
        val commentDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "unauth comment")

        mockMvc.perform(
            post("/api/v1/curations/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(commentDto))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("401-1"))
            .andExpect(jsonPath("$.msg").value("접근이 거부되었습니다. 로그인 상태를 확인해 주세요."))

        assertThat(commentService.getCommentsByCurationId(1L))
            .noneMatch { it.content == "unauth comment" }
    }

    @Test
    @DisplayName("댓글을 조회할 수 있다")
    fun getCommentsByCurationId() {
        createCommentAtCuration(1L, author)

        mockMvc.perform(get("/api/v1/curations/1/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("댓글이 조회되었습니다."))
            .andExpect(jsonPath("$.data[0].id").value("1"))
            .andExpect(jsonPath("$.data[0].authorName").value("other"))
            .andExpect(jsonPath("$.data[0].content").value("정말 유용한 정보네요! 감사합니다."))
    }

    @Test
    @DisplayName("답글을 작성할 수 있다")
    fun createReply() {
        
        val replyDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "답글 테스트")

        mockMvc.perform(
            post("/api/v1/curations/1/comments/1/reply")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(replyDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("댓글의 답글이 작성되었습니다."))
            .andExpect(jsonPath("$.data.content").value("답글 테스트"))
    }

    @Test
    @DisplayName("답글을 수정할 수 있다")
    fun updateReply() {
        val comment = createCommentAtCuration(1L, author)
        val replyDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "원래 답글")

        val response = mockMvc.perform(
            post("/api/v1/curations/1/comments/${comment.id}/reply")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(replyDto))
        ).andReturn().response.contentAsString

        val replyId = JsonPath.read<Int>(response, "$.data.id").toLong()

        mockMvc.perform(
            put("/api/v1/curations/1/comments/${comment.id}/reply/$replyId")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "content": "수정된 답글" }""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("답글이 수정되었습니다."))
            .andExpect(jsonPath("$.data.content").value("수정된 답글"))
    }


    @Test
    @DisplayName("실패 - 다른 사용자는 답글을 삭제할 수 없다")
    fun deleteOthersReply() {
        val other = memberRepository.findById(2L).get()
        val otherToken = authTokenService.genAccessToken(other)

        val commentDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "댓글")
        val commentResponse = mockMvc.perform(
            post("/api/v1/curations/1/comments")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(commentDto))
        ).andReturn().response.contentAsString
        val commentId = JsonPath.read<Any>(commentResponse, "$.data.id").toString().toLong()

        val replyDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "남의 댓글")
        val replyResponse = mockMvc.perform(
            post("/api/v1/curations/1/comments/$commentId/reply")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(replyDto))
        ).andReturn().response.contentAsString
        val replyId = JsonPath.read<Int>(replyResponse, "$.data.id").toString().toLong()

        mockMvc.perform(
            delete("/api/v1/curations/1/comments/$commentId/reply/$replyId")
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("403-2"))
            .andExpect(jsonPath("$.msg").value("답글을 삭제할 권한이 없습니다."))
    }


    private fun createCommentAtCuration(curationId: Long, author: Member): CommentDto {
        val commentDto = CommentDto(
            authorName = "테스트유저",
            authorProfileImageUrl = "https://example.com/image.jpg",
            content = "content example")
        return commentService.createComment(author, curationId, commentDto)
    }

    @Test
    @DisplayName("댓글 작성자는 댓글을 삭제할 수 있다")
    fun deleteComment() {
        val savedComment = createCommentAtCuration(1L, author)

        mockMvc.perform(
            delete("/api/v1/curations/1/comments/${savedComment.id}")
                .header("Authorization", "Bearer $authorAccessKey")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("댓글이 삭제되었습니다."))
    }

    @Test
    @DisplayName("실패 - 다른 사람의 댓글을 삭제할 수 없다")
    fun deleteOthersComment() {
        val otherAuthor = memberRepository.findById(2L).get()
        val savedComment = createCommentAtCuration(1L, otherAuthor)

        mockMvc.perform(
            delete("/api/v1/curations/1/comments/${savedComment.id}")
                .header("Authorization", "Bearer $authorAccessKey")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("403-2"))
            .andExpect(jsonPath("$.msg").value("댓글을 삭제할 권한이 없습니다."))
    }

    @Test
    @DisplayName("댓글 작성자는 댓글을 수정할 수 있다")
    fun updateComment() {
        val savedComment = createCommentAtCuration(1L, author)

        mockMvc.perform(
            put("/api/v1/curations/1/comments/${savedComment.id}")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "content": "new content" }""".trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("200-1"))
            .andExpect(jsonPath("$.msg").value("댓글이 수정되었습니다."))
    }


    @Test
    @DisplayName("실패 - 다른 사람의 댓글을 수정할 수 없다")
    fun updateOthersComment() {
        val otherAuthor = memberRepository.findById(2L).get()
        val savedComment = createCommentAtCuration(1L, otherAuthor)

        mockMvc.perform(
            put("/api/v1/curations/1/comments/${savedComment.id}")
                .header("Authorization", "Bearer $authorAccessKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "content": "new content" }""".trimIndent())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("403-2"))
            .andExpect(jsonPath("$.msg").value("댓글을 수정할 권한이 없습니다."))
    }
}