@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.team8.project2.domain.report.controller

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.report.entity.Report
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.AuthTokenService
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest
    @Autowired
    constructor(
        val mockMvc: MockMvc,
        val memberRepository: MemberRepository,
        val authTokenService: AuthTokenService,
        val reportRepository: ReportRepository,
        val curationRepository: CurationRepository,
    ) {
        private lateinit var accessToken: String
        private lateinit var reporter: Member

        @BeforeEach
        fun setUp() {
            reporter = memberRepository.findById(2L).orElseThrow()
            accessToken = authTokenService.genAccessToken(reporter)
        }

        @Test
        @DisplayName("신고한 큐레이션 목록을 조회할 수 있다")
        fun getMyReportedCurations() {
            val curation: Curation = curationRepository.findById(1L).orElseThrow()
            reportRepository.save(Report(
                curation = curation,
                reportType = ReportType.ABUSE,
                reporter = reporter,
            ))

            mockMvc
                .get("/api/v1/reports/myreported/${reporter.id}") {
                    header("Authorization", "Bearer $accessToken")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code", `is`("200-1"))
                    jsonPath("$.data[0].curationId", `is`(curation.id!!.toInt()))
                    jsonPath("$.data[0].reportType", `is`("ABUSE"))
                }
        }

        @Test
        @DisplayName("신고 내역이 없을 경우 빈 배열을 반환한다")
        fun getEmptyReportedCurations() {
            mockMvc
                .get("/api/v1/reports/myreported/${reporter.id}") {
                    header("Authorization", "Bearer $accessToken")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.length()", `is`(0))
                }
        }

        @Test
        @DisplayName("다른 사용자의 신고 내역은 조회할 수 없다")
        fun cannotAccessOthersReportList() {
            mockMvc
                .get("/api/v1/reports/myreported/3") {
                    header("Authorization", "Bearer $accessToken")
                }.andExpect {
                    status { is4xxClientError() }
                    jsonPath("$.code", `is`("403-1"))
                }
        }
    }
