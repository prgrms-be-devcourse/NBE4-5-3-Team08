package com.team8.project2.domain.member.service

import com.team8.project2.domain.member.entity.Member
import com.team8.project2.standard.util.Ut
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.Map

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthTokenServiceTest {
    @Autowired
    private val authTokenService: AuthTokenService? = null

    @Autowired
    private val memberService: MemberService? = null

    @Value("\${custom.jwt.secret-key}")
    private val keyString: String? = null

    @Value("\${custom.jwt.expire-seconds}")
    private val expireSeconds = 0

    private var testMember: Member? = null

    @BeforeEach
    fun setUp() {
        testMember = memberService!!.findByLoginId("loginId")
        //예외 throw 삭제함
    }

    @Test
    @DisplayName("jwt access token 생성 및 페이로드 검증")
    fun createAccessToken() {
        // Access Token 생성
        val accessToken = authTokenService!!.genAccessToken(testMember!!)

        Assertions.assertThat(accessToken).isNotBlank()

        // Payload 검증
        val parsedPayload = authTokenService.getPayload(accessToken)
        Assertions.assertThat(parsedPayload).containsEntry("id", testMember!!.id)
        Assertions.assertThat(parsedPayload).containsEntry("loginId", testMember!!.getLoginId())

        println("AccessToken = $accessToken")
    }

    @Test
    @DisplayName("jwt access token 유효성 검사")
    fun validateAccessToken() {
        // 토큰 생성
        val accessToken = authTokenService!!.genAccessToken(testMember!!)

        // 토큰이 유효한지 확인
        val isValid = Ut.Jwt.isValidToken(keyString.toString(), accessToken)
        Assertions.assertThat(isValid).isTrue()
    }

    @Test
    @DisplayName("jwt 만료된 토큰 검사")
    @Throws(InterruptedException::class)
    fun expiredTokenCheck() {
        // 테스트용 만료 시간 1초로 설정 후 토큰 생성
        val shortLivedToken = Ut.Jwt.createToken(keyString.toString(), 1, Map.of<String, Any>("id", testMember!!.id))

        // 2초 대기하여 토큰 만료
        Thread.sleep(2000)

        // 만료된 토큰인지 확인
        val isValid = Ut.Jwt.isValidToken(keyString.toString(), shortLivedToken)
        Assertions.assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("잘못된 토큰 검증")
    fun invalidTokenCheck() {
        val invalidToken = "this.is.a.fake.token"

        // 잘못된 토큰은 유효하지 않아야 함
        val isValid = Ut.Jwt.isValidToken(keyString.toString(), invalidToken)
        Assertions.assertThat(isValid).isFalse()
    }
}