package com.team8.project2.domain.member.service

import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.image.service.S3Uploader
import com.team8.project2.domain.member.entity.Follow
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.repository.FollowRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.ServiceException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(MockitoExtension::class)
class MemberServiceTest {
    @InjectMocks
    lateinit var memberService: MemberService

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var authTokenService: AuthTokenService

    @Mock
    lateinit var followRepository: FollowRepository

    @Mock
    lateinit var rq: Rq

    @Mock
    lateinit var curationRepository: CurationRepository

    @Mock
    lateinit var s3Uploader: S3Uploader

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Test
    @DisplayName("회원 가입 - null 가능 필드가 모두 채워졌을 때 정상 저장된다.")
    fun join_Succeeds_WhenAllNullableFieldsAreFilled() {
        val member = Member(
            loginId = "fullUser",
            username = "Full Name",
            password = "fullpw",
            role = RoleEnum.MEMBER,
            profileImage = "fullImage.png",
            email = "full@test.com",
            introduce = "Hello, I'm full user!"
        )
        whenever(
            memberRepository.save(
                ArgumentMatchers.any(
                    Member::class.java
                )
            )
        ).thenReturn(member)

        val result = memberService!!.join(
            "fullUser", "fullpw", RoleEnum.MEMBER, "full@test.com", "fullImage.png"
        )

        Assertions.assertNotNull(result)
        Assertions.assertEquals("fullUser", result.getLoginId())
        Assertions.assertEquals("Full Name", result.getUsername())
        Assertions.assertEquals("full@test.com", result.email)
        Assertions.assertEquals("fullImage.png", result.profileImage)
    }

    @Test
    @DisplayName("회원 가입 - null 가능 필드가 모두 비어 있을 때 정상 저장된다.")
    fun join_Succeeds_WhenAllNullableFieldsAreNull() {
        val member = Member(
            loginId = "minimalUser",
            username = "username",
            password = "minpw",
            role = RoleEnum.MEMBER,
            profileImage = null,
            email = null,
            introduce = null
        )

        whenever(
            memberRepository.save(
                ArgumentMatchers.any(
                    Member::class.java
                )
            )
        ).thenReturn(member)

        val result = memberService!!.join("minimalUser", "minpw", null, null, null)

        Assertions.assertNotNull(result)
        Assertions.assertEquals("minimalUser", result.getLoginId())
        Assertions.assertEquals("username", result.getUsername())
        Assertions.assertNull(result.email)
        Assertions.assertNull(result.profileImage)
    }

    @Test
    @DisplayName("회원 삭제 - 존재하는 회원이면 정상적으로 삭제된다.")
    fun deleteMember_Succeeds_WhenMemberExists() {
        // given
        val member = Member(loginId = "user1")

        whenever(memberRepository.findByLoginId("user1")).thenReturn(member)

        // when
        memberService!!.deleteMember("user1")

        // then
        verify(memberRepository).delete(member)
    }

    @Test
    @DisplayName("JWT 발급 - 회원 정보를 이용해 accessToken을 생성한다.")
    fun authToken_ReturnsAccessToken_WhenMemberIsValid() {
        val member = Member(1L, "user")
        whenever(authTokenService.genAccessToken(member)).thenReturn("mocked.jwt.token")

        val token = memberService!!.getAuthToken(member)

        Assertions.assertEquals("mocked.jwt.token", token)
    }

    @Test
    @DisplayName("JWT 인증 - accessToken의 payload로부터 사용자 정보를 생성한다.")
    fun memberByAccessToken_ReturnsMember_WhenPayloadIsValid() {
        val payload = mapOf<String, Any>("id" to 1L, "loginId" to "user")
        whenever(authTokenService.getPayload("token123")).thenReturn(payload)

        val result = memberService!!.getMemberByAccessToken("token123")

        Assertions.assertTrue(result.isPresent)
        Assertions.assertEquals(1L, result.get().id)
        Assertions.assertEquals("user", result.get().getLoginId())
    }


    @Test
    @DisplayName("팔로우 - 정상적으로 팔로우 관계가 저장된다.")
    fun followUser_Succeeds_WhenValidFolloweeIsGiven() {
        val follower = Member(1L, "user1")
        val followee = Member(2L, "user2")


        whenever(memberRepository.findByUsername("user2")).thenReturn(followee)
        whenever(followRepository.findByFollowerAndFollowee(follower, followee)).thenReturn(null)
        val follow = Follow()
        follow.setFollowerAndFollowee(follower, followee)
        whenever(followRepository.save(any<Follow>())).thenReturn(follow)
        val result = memberService!!.followUser(follower, "user2")

        Assertions.assertNotNull(result)
        verify(followRepository).save(ArgumentMatchers.any())
    }

    @Test
    @DisplayName("팔로우 - 자기 자신은 팔로우할 수 없다.")
    fun followUser_Fails_WhenFollowSelf() {
        val follower = Member(1L, "user1")

        // followee도 동일한 user1로 세팅
        whenever(memberRepository.findByUsername("user1")).thenReturn(follower)

        val ex = Assertions.assertThrows(
            ServiceException::class.java
        ) { memberService!!.followUser(follower, "user1") }

        Assertions.assertEquals("400-1", ex.code)
    }
}
