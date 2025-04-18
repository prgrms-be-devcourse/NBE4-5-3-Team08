package com.team8.project2.domain.curation.controller

import com.team8.project2.domain.curation.curation.dto.CurationDetailResDto
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.global.RedisUtils
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LikeCurationConcurrencyTest {

    @Autowired
    private lateinit var curationService: CurationService

    @Autowired
    private lateinit var curationRepository: CurationRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var redisUtils: RedisUtils

    private var testCurationId: Long = 0
    private var testLoginId: Long = 0

    @BeforeEach
    fun setUp() {
        redisUtils.clearAllData()

        val curation = curationRepository.findById(1L).get()
        testCurationId = curation.id!!

        val member = memberRepository.findById(1L).get()
        testLoginId = member.id!!
    }

    @Test
    fun testConcurrentLikes() {
        val threadCount = 1000
        val executorService = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executorService.submit {
                try {
                    curationService.likeCuration(testCurationId, testLoginId)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        val dto: CurationDetailResDto = curationService.getCuration(testCurationId, request)
        println("Final like count: ${dto.likeCount}")

        assertThat(dto.likeCount).isEqualTo(0L)
    }
}
