package com.team8.project2.domain.comment.controller;

import com.team8.project2.domain.comment.entity.Comment;
import com.team8.project2.domain.comment.repository.CommentRepository;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.member.service.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiV1GlobalCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AuthTokenService authTokenService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("BaseInitData: memberId=1 not found"));
        accessToken = authTokenService.genAccessToken(author);
    }

    @Test
    @DisplayName("BaseInitData 기반 - 댓글이 있는 사용자(other)가 자신의 댓글을 삭제할 수 있다")
    void deleteCommentOfOther() throws Exception {
        // memberId=2L이 작성한 댓글을 찾는다
        List<Comment> comments = commentRepository.findAll();
        Comment myComment = comments.stream()
                .filter(c -> c.getAuthor().getId().equals(2L))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("BaseInitData: memberId=2의 댓글 없음"));

        // 삭제 요청
        mockMvc.perform(delete("/api/v1/comments/{id}", myComment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("댓글이 삭제되었습니다."));

        assertThat(commentRepository.findById(myComment.getId())).isEmpty();
    }


    @Test
    @DisplayName("BaseInitData 기반 - 내 댓글 조회")
    void getMyCommentsList() throws Exception {
        Member member1 = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("BaseInitData: memberId=1 not found"));
        String tokenForUser1 = authTokenService.genAccessToken(member1);

        // 사전 검증
        long count = commentRepository.findAll().stream()
                .filter(c -> c.getAuthor().getId().equals(1L))
                .count();
        System.out.println("✅ memberId=1 작성한 댓글 수: " + count); // member Id : 1의 댓글 개수는 현재 0이므로 0으로 출력됨

        mockMvc.perform(get("/api/v1/comments/mycomments")
                        .header("Authorization", "Bearer " + tokenForUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 댓글 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value((int) count)); // 동적 대응

        if (count > 0) {
            mockMvc.perform(get("/api/v1/comments/mycomments")
                            .header("Authorization", "Bearer " + tokenForUser1))
                    .andExpect(jsonPath("$.data[0].authorId").value(1L));
        }
    }

}
