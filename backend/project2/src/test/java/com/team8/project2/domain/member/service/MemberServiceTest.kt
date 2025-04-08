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
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import java.util.*
import java.util.Map

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(MockitoExtension::class)
class MemberServiceTest {
    @InjectMocks
    private val memberService: MemberService? = null

    @Mock
    private val memberRepository: MemberRepository? = null

    @Mock
    private val authTokenService: AuthTokenService? = null

    @Mock
    private val followRepository: FollowRepository? = null

    @Mock
    private lateinit var rq: Rq

    @Mock
    private lateinit var curationRepository: CurationRepository

    @Mock
    private lateinit var s3Uploader: S3Uploader

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Test
    @DisplayName("회원 가입 - null 가능 필드가 모두 채워졌을 때 정상 저장된다.")
    fun join_Succeeds_WhenAllNullableFieldsAreFilled() {
        val member = Member(
            "fullUser",  // memberId
            "Full Name",  // username
            "fullpw",  // password
            RoleEnum.MEMBER,  // roleEnum
            "fullImage.png",  // profileImage
            "full@test.com",  // email
            "Hello, I'm full user!" // introduce
        )

        whenever(
            memberRepository!!.save(
                ArgumentMatchers.any(
                    Member::class.java
                )
            )
        ).thenReturn(member)

        val result = memberService!!.join(
            "fullUser", "fullpw", RoleEnum.MEMBER, "full@test.com", "fullImage.png"
        )

        Assertions.assertNotNull(result)
        Assertions.assertEquals("fullUser", result.getMemberId())
        Assertions.assertEquals("Full Name", result.getUsername())
        Assertions.assertEquals("full@test.com", result.email)
        Assertions.assertEquals("fullImage.png", result.profileImage)
    }

    @Test
    @DisplayName("회원 가입 - null 가능 필드가 모두 비어 있을 때 정상 저장된다.")
    fun join_Succeeds_WhenAllNullableFieldsAreNull() {
        val member = Member(
            "minimalUser",  // memberId
            "username", // username
            "minpw",  // password
            RoleEnum.MEMBER,  // roleEnum
            null,  // email
            null,  // profileImage
            null // introduce
        )

        whenever(
            memberRepository!!.save(
                ArgumentMatchers.any(
                    Member::class.java
                )
            )
        ).thenReturn(member)

        val result = memberService!!.join("minimalUser", "minpw", null, null, null)

        Assertions.assertNotNull(result)
        Assertions.assertEquals("minimalUser", result.getMemberId())
        Assertions.assertEquals("username", result.getUsername())
        Assertions.assertNull(result.email)
        Assertions.assertNull(result.profileImage)
    }

    @Test
    @DisplayName("회원 삭제 - 존재하는 회원이면 정상적으로 삭제된다.")
    fun deleteMember_Succeeds_WhenMemberExists() {
        // given
        val member = Member("user1")

        whenever(memberRepository!!.findByMemberId("user1")).thenReturn(Optional.of(member))

        // when
        memberService!!.deleteMember("user1")

        // then
        verify(memberRepository).delete(member)
    }

    @Test
    @DisplayName("JWT 발급 - 회원 정보를 이용해 accessToken을 생성한다.")
    fun authToken_ReturnsAccessToken_WhenMemberIsValid() {
        val member = Member(1L, "user")
        whenever(authTokenService!!.genAccessToken(member)).thenReturn("mocked.jwt.token")

        val token = memberService!!.getAuthToken(member)

        Assertions.assertEquals("mocked.jwt.token", token)
    }

    @Test
    @DisplayName("JWT 인증 - accessToken의 payload로부터 사용자 정보를 생성한다.")
    fun memberByAccessToken_ReturnsMember_WhenPayloadIsValid() {
        val payload = mapOf<String, Any>("id" to 1L, "memberId" to "user")
        whenever(authTokenService!!.getPayload("token123")).thenReturn(payload)

        val result = memberService!!.getMemberByAccessToken("token123")

        Assertions.assertTrue(result.isPresent)
        Assertions.assertEquals(1L, result.get().id)
        Assertions.assertEquals("user", result.get().getMemberId())
    }


    @Test
    @DisplayName("팔로우 - 정상적으로 팔로우 관계가 저장된다.")
    fun followUser_Succeeds_WhenValidFolloweeIsGiven() {
        val follower = Member(1L, "user1")
        val followee = Member(2L, "user2")


        whenever(memberRepository!!.findByUsername("user2")).thenReturn(Optional.of(followee))
        whenever(followRepository!!.findByFollowerAndFollowee(follower, followee)).thenReturn(Optional.empty())
        val follow = Follow()
        follow.setFollowerAndFollowee(follower, followee)
        whenever(followRepository!!.save(any<Follow>())).thenReturn(follow)
        val result = memberService!!.followUser(follower, "user2")

        Assertions.assertNotNull(result)
        verify(followRepository).save(ArgumentMatchers.any())
    }

    @Test
    @DisplayName("팔로우 - 자기 자신은 팔로우할 수 없다.")
    fun followUser_Fails_WhenFollowSelf() {
        val follower = Member(1L, "user1")

        // followee도 동일한 user1로 세팅
        whenever(memberRepository!!.findByUsername("user1")).thenReturn(Optional.of(follower))

        val ex = Assertions.assertThrows(
            ServiceException::class.java
        ) { memberService!!.followUser(follower, "user1") }

        Assertions.assertEquals("400-1", ex.code)
    }
}
