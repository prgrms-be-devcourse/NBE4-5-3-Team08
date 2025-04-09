package com.team8.project2.domain.admin.controller

import com.team8.project2.domain.admin.service.AdminService
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("관리자 API 테스트")
class ApiV1AdminControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val adminService: AdminService,
    private val memberRepository: MemberRepository,
    private val authTokenService: AuthTokenService,
    private val curationRepository: CurationRepository
) {

    private lateinit var memberAccessKey: String
    private lateinit var member: Member

    @BeforeEach
    fun setUp() {
        member = memberRepository.findById(4L).get()
        memberAccessKey = authTokenService.genAccessToken(member)
    }

    @Test
    @DisplayName("큐레이션 삭제 - 관리자 권한으로 큐레이션을 삭제할 수 있다.")
    fun deleteCuration_ShouldReturnSuccessResponse() {
        val savedCuration = curationRepository.findById(1L).orElseThrow()

        mockMvc.delete("/api/v1/admin/curations/{curationId}", savedCuration.id) {
            header("Authorization", "Bearer $memberAccessKey")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @DisplayName("멤버 삭제 - 관리자 권한으로 멤버를 삭제할 수 있다.")
    fun deleteMember_ShouldReturnSuccessResponse() {
        mockMvc.delete("/api/v1/admin/members/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("200-1") }
                jsonPath("$.msg") { value("멤버가 삭제되었습니다.") }
            }
    }

    @Test
    @DisplayName("신고된 큐레이션 조회 - 일정 개수 이상 신고된 큐레이션을 조회할 수 있다.")
    fun getReportedCurations_ShouldReturnListOfIds() {
        mockMvc.get("/api/v1/admin/reported-curations") {
            param("minReports", "5")
            header("Authorization", "Bearer $memberAccessKey")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("200-1") }
            jsonPath("$.data") { isArray() }
        }
    }

    @Test
    @DisplayName("통계 조회 - 큐레이션 및 플레이리스트의 조회수와 좋아요 수를 확인할 수 있다.")
    fun getCurationAndPlaylistStats_ShouldReturnStats() {
        mockMvc.get("/api/v1/admin/stats") {
            header("Authorization", "Bearer $memberAccessKey")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("200-1") }
            jsonPath("$.data.totalCurationViews") { exists() }
            jsonPath("$.data.totalPlaylistViews") { exists() }
            jsonPath("$.data.totalCurationLikes") { exists() }
            jsonPath("$.data.totalPlaylistLikes") { exists() }
        }
    }
}
