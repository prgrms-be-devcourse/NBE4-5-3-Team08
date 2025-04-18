package com.team8.project2.domain.member.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.image.service.S3Uploader
import com.team8.project2.domain.member.dto.MemberReqDTO
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.exception.ServiceException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val memberService: MemberService,
    private val memberRepository: MemberRepository,
    private val curationService: CurationService,
    private val s3Uploader: S3Uploader
) {
    private val memberReqDTO: MemberReqDTO? = null

    @Throws(Exception::class)
    private fun joinRequest(
        loginId: String,
        password: String,
        email: String,
        role: String,
        profileImage: String,
        introduce: String
    ): ResultActions {
        return mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/members/join")
                    .content(
                        """
                                        {
                                            "loginId": "${loginId}",
                                            "password": "${password}",
                                            "username": "${loginId}",
                                            "email": "${email}",
                                            "role": "${role}",
                                            "profileImage": "${profileImage}",
                                            "introduce": "${introduce}"
                                        }
                                        
                                        """
                            .trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("회원 가입1")
    @Throws(Exception::class)
    fun join1() {
        val loginId = "user123"
        val password = "securePass123"
        val email = "user123@example.com"
        val role = "MEMBER" // RoleEnum이 문자열로 변환됨
        val profileImage = "https://example.com/profile.jpg"
        val introduce = "안녕하세요! 저는 새로운 회원입니다."

        val resultActions = joinRequest(loginId, password, email, role, profileImage, introduce)
        //입력 확인
        val member = memberService.findByLoginId("user123")
            ?: throw ServiceException("404-1", "해당 회원을 찾을 수 없습니다.")


        resultActions
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 가입이 완료되었습니다."))
    }

    @Test
    @DisplayName("회원 가입2 - username이 이미 존재하는 케이스")
    @Throws(Exception::class)
    fun join2() {
        val loginId = "user123"
        val password = "securePass123"
        val email = "user123@example.com"
        val role = "MEMBER" // RoleEnum이 문자열로 변환됨
        val profileImage = "https://example.com/profile.jpg"
        val introduce = "안녕하세요! 저는 새로운 회원입니다."

        // 회원 가입 요청
        val resultActions = joinRequest(loginId, password, email, role, profileImage, introduce)
        // 같은 loginId를 통한 회원 가입 재요청
        val resultActions2 = joinRequest(loginId, password, email, role, profileImage, introduce)
        resultActions2
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("409-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("사용중인 아이디"))
    }

    @Test
    @DisplayName("회원 가입3 - 비밀번호 길이 제한 위반")
    @Throws(Exception::class)
    fun join3() {
        // 필수 입력값 중 password를 4자로 설정하여 길이 제한 검증
        val memberReqDTO = MemberReqDTO(
            "member" + UUID.randomUUID(),
            "1234",  // 비밀번호 4자 → 에러 유도
            "member1@gmail.com",
            "user" + UUID.randomUUID(),
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )


        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(memberReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("password : Size : 비밀번호는 최소 6자 이상이어야 합니다."))
    }

    @Test
    @DisplayName("회원 가입4 - 필수 입력값 누락 (비밀번호 없음)")
    @Throws(Exception::class)
    fun joinWithoutPassword() {
        // 비밀번호 누락 테스트
        val memberReqDTO = MemberReqDTO(
            "member" + UUID.randomUUID(),
            null,
            "member1@gmail.com",
            "user" + UUID.randomUUID(),
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(memberReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("password : NotBlank : 비밀번호는 필수 입력값입니다."))
    }

    @Test
    @DisplayName("회원 가입5 - 필수 입력값 누락 (사용자명 없음)")
    @Throws(Exception::class)
    fun joinWithoutLoginId() {
        // 사용자명 누락 테스트
        val memberReqDTO = MemberReqDTO(
            null,
            "123456",  // 비밀번호 4자 → 에러 유도
            "member1@gmail.com",
            null,
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ObjectMapper().writeValueAsString(memberReqDTO))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("loginId : NotBlank : 회원 ID는 필수 입력값입니다."))
    }

    @Test
    @DisplayName("username 기반 큐레이터 정보 조회")
    @Throws(Exception::class)
    fun curatorInfoTest() {
        val savedMember = memberRepository.findById(1L).orElseThrow()
        val curationCount = curationService.countByMember(savedMember)

        // When
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/members/${savedMember.getUsername()}"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-4"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value("username"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.profileImage")
                    .value(s3Uploader.baseUrl + "default-profile.svg")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.introduce").value("test"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.curationCount").value(curationCount)
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("이미 존재하는 loginId로 회원 가입 시 오류 발생")
    @Throws(
        Exception::class
    )
    fun joinWithDuplicateLoginId() {
        // Given
        val duplicateLoginId = "loginId" // BaseInitData 에서 이미 생성된 ID 사용
        val password = "123456"
        val email = "duplicate@example.com"
        val role = "MEMBER"
        val profileImage = "www.url"
        val introduce = "안녕"

        // When
        val resultActions = joinRequest(duplicateLoginId, password, email, role, profileImage, introduce)

        // Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("409-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("사용중인 아이디"))
    }


    @Test
    @DisplayName("JWT 인증으로 내 정보 조회")
    @Throws(Exception::class)
    fun myInfoTest() {
        val memberReqDTO = MemberReqDTO(
            "member" + UUID.randomUUID(),
            "1234",  // 비밀번호 4자 → 에러 유도
            "member1@gmail.com",
            "user" + UUID.randomUUID(),
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )

        val member = memberService.join(memberReqDTO.toEntity())
        val accessToken = memberService.genAccessToken(member)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-2"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("로그아웃 시 JWT 삭제")
    @Throws(Exception::class)
    fun logoutTest() {
        // MemberReqDTO 설정 (링크 포함)
        val memberReqDTO = MemberReqDTO(
            "member" + UUID.randomUUID(),
            "1234",  // 비밀번호 4자 → 에러 유도
            "member1@gmail.com",
            "user" + UUID.randomUUID(),
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )


        val member = memberService.join(memberReqDTO.toEntity())
        val accessToken = memberService.genAccessToken(member)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-3"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("회원 정보 업데이트 - 권한 있는 사용자가 본인 정보 수정 성공")
    @Throws(
        Exception::class
    )
    fun updateMember_success() {
        //setup
        val memberReqDTO = MemberReqDTO(
            "member" + UUID.randomUUID(),
            "1234",  // 비밀번호 4자 → 에러 유도
            "member1@gmail.com",
            "user" + UUID.randomUUID(),
            "www.url",
            "안녕",
            RoleEnum.MEMBER
        )

        // Given
        val member = memberService.join(memberReqDTO.toEntity())
        val accessToken = memberService.genAccessToken(member)

        // 수정할 정보
        val newUsername = "수정된이름"
        val newEmail = "updated@email.com"
        val newIntroduce = "자기소개 수정 테스트"

        // When
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/members/{loginId}", member.getLoginId())
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "loginId": "${member.getLoginId()}",
                  "username": "${newUsername}",
                  "email": "${newEmail}",
                  "introduce": "${newIntroduce}"
                }
                """.trimIndent()
                )
        ) // Then
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-5"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 정보가 수정되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value(newUsername))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(newEmail))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.introduce").value(newIntroduce))
            .andDo(MockMvcResultHandlers.print())
    }

    @Nested
    @DisplayName("팔로우")
    internal inner class Follow {
        @Test
        @DisplayName("다른 사용자를 팔로우할 수 있다")
        @Throws(Exception::class)
        fun follow() {
            val followeeId = 1L
            val followerId = 2L
            val followee = memberService.findById(followeeId)
            val member = memberService.findById(followerId)
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${followee.getUsername()}/follow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.msg").value("${followee.getUsername()}님을 팔로우했습니다.")
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.followee").value(followee.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.followedAt").isNotEmpty())
        }

        @Test
        @DisplayName("실패 - 이미 팔로우중인 사용자를 팔로우할 수 없다")
        @Throws(
            Exception::class
        )
        fun follow_alreadyFollowed() {
            val followeeId = 1L
            val followerId = 3L
            val followee = memberService.findById(followeeId)
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${followee.getUsername()}/follow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 팔로우중인 사용자입니다."))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자를 팔로우할 수 없다")
        @Throws(
            Exception::class
        )
        fun follow_invalidFollowee() {
            val invalidFolloweeLoginId = "invalidLoginId"
            val followerId = 1L
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${invalidFolloweeLoginId}/follow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("404-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 사용자입니다."))
        }

        @Test
        @DisplayName("실패 - 자신을 팔로우할 수 없다")
        @Throws(Exception::class)
        fun follow_self() {
            val followeeId = 1L
            val followerId = 1L
            val followee = memberService.findById(followeeId)
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${followee.getUsername()}/follow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("자신을 팔로우할 수 없습니다."))
        }

        @Test
        @DisplayName("팔로우중인 다른 사용자를 팔로우 취소할 수 있다")
        @Throws(Exception::class)
        fun unfollow() {
            val followeeId = 1L
            val followerId = 3L
            val followee = memberService.findById(followeeId)
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${followee.getUsername()}/unfollow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.msg").value("${followee.getUsername()}님을 팔로우 취소했습니다.")
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.followee").value(followee.getUsername()))
        }

        @Test
        @DisplayName("실패 - 팔로우중이 아닌 사용자를 팔로우 취소하면 실패한다")
        @Throws(
            Exception::class
        )
        fun unfollow_notFollowed() {
            val followeeId = 3L
            val followerId = 1L
            val followee = memberService.findById(followeeId)
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/members/${followee.getUsername()}/unfollow")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("팔로우중이 아닙니다."))
        }

        @Test
        @DisplayName("팔로우중인 사용자를 조회할 수 있다")
        @Throws(Exception::class)
        fun following() {
            val followee1Id = 1L
            val followee2Id = 2L
            val followerId = 3L
            val followee1 = memberService.findById(followee1Id)
            val followee2 = memberService.findById(followee2Id)
            val member = memberRepository.findById(followerId).get()
            val accessToken = memberService.genAccessToken(member)

            mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/members/following")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("팔로우 중인 사용자를 조회했습니다."))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.following[0].followee").value(followee2.getUsername())
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.following[0].followedAt").isNotEmpty())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.following[1].followee").value(followee1.getUsername())
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.following[1].followedAt").isNotEmpty())
        }
    }
}