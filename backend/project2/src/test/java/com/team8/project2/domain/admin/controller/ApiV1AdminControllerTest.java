package com.team8.project2.domain.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.curation.repository.CurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.team8.project2.domain.admin.service.AdminService;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.member.service.AuthTokenService;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("관리자 API 테스트")
class ApiV1AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;


    @Autowired
    private CurationRepository curationRepository;

    String memberAccessKey;
    Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.findById(4L).get();
        memberAccessKey = authTokenService.genAccessToken(member);
    }


    // ✅ 큐레이션 삭제 API 테스트
    @Test
    @DisplayName("큐레이션 삭제 - 관리자 권한으로 큐레이션을 삭제할 수 있다.")
    void deleteCuration_ShouldReturnSuccessResponse() throws Exception {
        Curation savedCuration = curationRepository.findById(1L).orElseThrow();

        mockMvc.perform(delete("/api/v1/admin/curations/{curationId}", savedCuration.getId())
                        .header("Authorization", "Bearer " + memberAccessKey))
                .andExpect(status().isNoContent());
    }

    // ✅ 멤버 삭제 API 테스트
    @Test
    @DisplayName("멤버 삭제 - 관리자 권한으로 멤버를 삭제할 수 있다.")
    void deleteMember_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("멤버가 삭제되었습니다."));
    }

    // ✅ 신고된 큐레이션 조회 API 테스트
    @Test
    @DisplayName("신고된 큐레이션 조회 - 일정 개수 이상 신고된 큐레이션을 조회할 수 있다.")
    void getReportedCurations_ShouldReturnListOfIds() throws Exception {

        mockMvc.perform(get("/api/v1/admin/reported-curations")
                        .param("minReports", "5")
                        .header("Authorization", "Bearer " + memberAccessKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ✅ 통계 조회 API 테스트
    @Test
    @DisplayName("통계 조회 - 큐레이션 및 플레이리스트의 조회수와 좋아요 수를 확인할 수 있다.")
    void getCurationAndPlaylistStats_ShouldReturnStats() throws Exception {
        // 실제 테스트 요청
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + memberAccessKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.data").exists())  // data 필드가 존재하는지 확인
                .andExpect(jsonPath("$.data.totalCurationViews").exists())  // totalCurationViews 필드 확인
                .andExpect(jsonPath("$.data.totalPlaylistViews").exists())  // 예시: totalPlaylistViews도 확인
                .andExpect(jsonPath("$.data.totalCurationLikes").exists())  // 예시: totalLikes 확인
                .andExpect(jsonPath("$.data.totalPlaylistLikes").exists()); // 예시: totalLikes 확인
    }
}
