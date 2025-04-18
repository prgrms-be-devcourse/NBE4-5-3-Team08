package com.team8.project2.domain.curation.service

import com.team8.project2.domain.curation.curation.dto.CurationDetailResDto
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.entity.CurationLink
import com.team8.project2.domain.curation.curation.entity.CurationTag
import com.team8.project2.domain.curation.curation.event.CurationUpdateEvent
import com.team8.project2.domain.curation.curation.repository.CurationLinkRepository
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.repository.CurationTagRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.curation.service.CurationViewService
import com.team8.project2.domain.curation.like.entity.Like
import com.team8.project2.domain.curation.like.repository.LikeRepository
import com.team8.project2.domain.curation.report.entity.ReportType
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.curation.tag.service.TagService
import com.team8.project2.domain.image.repository.CurationImageRepository
import com.team8.project2.domain.image.service.CurationImageService
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.service.LinkService
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.ServiceException
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import java.util.Optional
import java.util.Set

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(MockitoExtension::class)
internal class CurationServiceTest {

    @Mock
    lateinit var curationRepository: CurationRepository

    @Mock
    lateinit var curationLinkRepository: CurationLinkRepository

    @Mock
    lateinit var curationTagRepository: CurationTagRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var likeRepository: LikeRepository

    @Mock
    lateinit var linkService: LinkService

    @Mock
    lateinit var tagService: TagService

    @Mock
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    lateinit var reportRepository: ReportRepository

    @Mock
    lateinit var memberService: MemberService

    @Mock
    lateinit var curationViewService: CurationViewService

    @Mock
    lateinit var curationImageService: CurationImageService

    @Mock
    lateinit var curationImageRepository: CurationImageRepository

    @Mock
    lateinit var rq: Rq

    @InjectMocks
    lateinit var curationService: CurationService

    lateinit var curation: Curation

    lateinit var link: Link

    lateinit var tag: Tag

    lateinit var member: Member

    @BeforeEach
    fun setup() {
        member = Member(
            id = 1L,
            username = "testUser",
            email = "test@example.com"
        )

        curation = Curation(
            title = "Test Title",
            content = "Test Content",
            member = member,
        )
        ReflectionTestUtils.setField(curation, "id", 1L)

        link = Link(
            id = 1L,
            url = "https://test.com"
        )

        tag = Tag("test")
    }


    @Test
    @DisplayName("큐레이션을 생성할 수 있다")
    fun createCuration() {
        val urls = listOf("http://example.com", "http://another-url.com")
        val tags = listOf("tag1", "tag2", "tag3")

        val dummyMember = Member()

        whenever(linkService.getLink(any())).thenReturn(link)
        whenever(tagService.getTag(any())).thenReturn(tag)
        whenever(curationRepository.save(any())).thenReturn(curation)
        whenever(curationLinkRepository.saveAll(any<List<CurationLink>>())).thenReturn(listOf(CurationLink()))
        whenever(curationTagRepository.saveAll(any<List<CurationTag>>())).thenReturn(listOf(CurationTag()))

        val createdCuration = curationService.createCuration("New Title", "New Content", urls, tags, dummyMember)

        verify(curationRepository).save(any())
        verify(curationLinkRepository).saveAll(any<List<CurationLink>>())
        verify(curationTagRepository).saveAll(any<List<CurationTag>>())

        assertNotNull(createdCuration)
        assertEquals("New Title", createdCuration.title)
    }

    @Test
    @DisplayName("큐레이션을 수정할 수 있다")
    fun UpdateCuration() {
        // Given: 테스트를 위한 데이터 준비


        val urls: List<String> = mutableListOf("http://updated-url.com", "http://another-url.com")
        val tags: List<String> = mutableListOf("updated-tag1", "updated-tag2", "updated-tag3")

        // Mocking Curation 객체
        val curation = Curation(
            "Original Title",
            "Original Content",
            member
        )
        ReflectionTestUtils.setField(curation, "id", 1L)

        // Mocking 링크 및 태그
        val link = Link() // Link 객체를 생성하는 코드 필요 (예: getLink 메서드에서 반환할 객체 설정)
        val tag = Tag(
            id = 1L,
            name = "태그"
        ) // Tag 객체 생성 (예: getTag 메서드에서 반환할 객체 설정)

        // Mocking 리포지토리 및 서비스 호출
        whenever(curationRepository.findById(1L)).thenReturn(Optional.of(curation))
        whenever(linkService.getLink(any())).thenReturn(link)
        whenever(tagService.getTag(any())).thenReturn(tag)
        whenever(curationRepository.save(any<Curation>())).thenReturn(curation)
        whenever(curationLinkRepository.saveAll(any<List<CurationLink>>())).thenReturn(listOf(CurationLink()))
        whenever(curationTagRepository.saveAll(any<List<CurationTag>>())).thenReturn(listOf(CurationTag()))
        doNothing().`when`(eventPublisher).publishEvent(any<CurationUpdateEvent>())

        // When: 큐레이션 업데이트 호출
        val updatedCuration = curationService.updateCuration(
            1L, "Updated Title", "Updated Content", urls, tags,
            member
        )

        // Then: 상호작용 검증
        verify(curationRepository, times(1)).findById(1L)
        verify(curationRepository, times(1)).save(any<Curation>())
        verify(curationLinkRepository, times(1)).saveAll(any<List<CurationLink>>())
        verify(curationTagRepository, times(1)).saveAll(any<List<CurationTag>>())

        // 결과 확인
        assertNotNull(updatedCuration)
        assertEquals("Updated Title", updatedCuration.title)
        assertEquals("Updated Content", updatedCuration.content)
    }


    @Test
    @DisplayName("실패 - 존재하지 않는 큐레이션을 수정하면 실패한다")
    fun UpdateCurationNotFound() {
        val member = Member() // Member 객체 생성
        val urls: List<String?> = mutableListOf<String?>("http://updated-url.com")
        val tags: List<String?> = mutableListOf<String?>("tag1", "tag2", "tag3")

        // Mocking repository to return empty Optional
        whenever(curationRepository.findById(any())).thenReturn(Optional.empty())

        // Check if exception is thrown
        try {
            curationService.updateCuration(1L, "Updated Title", "Updated Content", urls, tags, member)
        } catch (e: ServiceException) {
            assert(e.message!!.contains("해당 큐레이션을 찾을 수 없습니다."))
        }
    }

    @Test
    @DisplayName("큐레이션을 삭제할 수 있다")
    fun deleteCuration() {
        // given
        whenever(curationRepository.findById(1L)).thenReturn(Optional.of(curation))

        doNothing().`when`(curationRepository).deleteById(any())
        doNothing().`when`(reportRepository).deleteByCurationId(any())

        val zSetOperations = mock<ZSetOperations<String, Any>>()
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)

        // when
        curationService.deleteCuration(1L, member)

        // then
        verify(curationRepository, times(1)).deleteById(1L)
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 큐레이션을 삭제할 수 없다")
    fun DeleteCurationNotFound() {
        // Mocking repository to return false for existence check
        whenever(curationRepository.findById(any())).thenReturn(Optional.empty())

        // Check if exception is thrown
        AssertionsForClassTypes.assertThatThrownBy {
            curationService.deleteCuration(
                1L,
                member
            )
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("해당 큐레이션을 찾을 수 없습니다.")

        // Verify that deleteById is never called because the curation does not exist
        verify(curationRepository, never()).deleteById(any<Long>())
    }


    @Test
    @DisplayName("큐레이션을 조회할 수 있다")
    fun GetCuration() {
        // HttpServletRequest 모킹
        val request = mock<HttpServletRequest>()
        whenever(request.remoteAddr).thenReturn("192.168.0.1") // IP를 임의로 설정

        val valueOperations: ValueOperations<String, Any> = mock<ValueOperations<String, Any>>()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        val setOperations: SetOperations<String, Any> = mock<SetOperations<String, Any>>()
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)

        // Mocking repository to return a Curation
        whenever(curationRepository.findById(any<Long>())).thenReturn(
            Optional.of(
                curation
            )
        )


        whenever(memberService.isFollowed(any(), any())).thenReturn(true)

        val rq = mock<Rq>()
        whenever(rq.isLogin).thenReturn(true)
        whenever(rq.actor).thenReturn(member)
        ReflectionTestUtils.setField(curationService, "rq", rq)

        val retrievedCuration: CurationDetailResDto = checkNotNull(curationService.getCuration(1L, request))

        assert(retrievedCuration.title == "Test Title")
    }

    @Test
    @DisplayName("큐레이션 조회수는 한 번만 증가해야 한다")
    fun GetCurationMultipleTimes() {
        // HttpServletRequest 모킹


        val request = mock<HttpServletRequest>()
        whenever(request.remoteAddr).thenReturn("192.168.0.1") // IP를 임의로 설정

        // Given: Redis와 큐레이션 관련 의존성 준비
        val valueOperations: ValueOperations<String, Any> = mock<ValueOperations<String, Any>>()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        val zSetOperations: ZSetOperations<String, Any> = mock<ZSetOperations<String, Any>>()
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        val setOperations: SetOperations<String, Any> = mock<SetOperations<String, Any>>()
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)

        // 큐레이션 조회 로직이 제대로 동작하도록 설정
        whenever(curationRepository.findById(any<Long>())).thenReturn(
            Optional.of(
                curation
            )
        )

        whenever(memberService.isFollowed(any(), any())).thenReturn(true)

        val rq = mock<Rq>()
        whenever(rq.isLogin).thenReturn(true)
        whenever(rq.actor).thenReturn(member)
        ReflectionTestUtils.setField(curationService, "rq", rq)

        doNothing().`when`(curationViewService).increaseViewCount(curation)

        // 첫 번째 조회에서만 true 반환하고, 그 이후에는 false 반환하도록 설정
        whenever(
            valueOperations.setIfAbsent(
                any(), eq("true"), eq(
                    Duration.ofDays(1)
                )
            )
        )
            .thenReturn(true) // 첫 번째 조회에서는 키가 없으므로 true 반환
            .thenReturn(false) // 두 번째 이후의 조회에서는 키가 이미 있으므로 false 반환


        // When: 큐레이션을 여러 번 조회한다
        curationService.getCuration(1L, request)
        curationService.getCuration(1L, request)
        curationService.getCuration(1L, request)

        // Then: 조회수는 한 번만 증가해야 한다
        verify(curationViewService, times(1)).increaseViewCount(curation)
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 큐레이션을 조회하면 실패한다")
    fun GetCurationNotFound() {
        // HttpServletRequest 모킹
        val request = mock<HttpServletRequest>()
        whenever(request.remoteAddr).thenReturn("192.168.0.1") // IP를 임의로 설정

        val valueOperations: ValueOperations<String, Any> = mock<ValueOperations<String, Any>>()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)

        // Mocking repository to return empty Optional
        whenever(curationRepository.findById(any())).thenReturn(Optional.empty())

        // Check if exception is thrown
        try {
            curationService.getCuration(1L, request)
        } catch (e: ServiceException) {
            assert(e.message!!.contains("해당 큐레이션을 찾을 수 없습니다."))
        }
    }

    @Test
    @DisplayName("큐레이션 좋아요 기능을 테스트합니다.")
    fun likeCuration() {
        val curationId = 1L
        val loginId = 1L

        val redisKey = "curation_like:$curationId"
        val redisValue = loginId.toString()

        // 실제 큐레이션과 멤버 객체
        val mockCuration = mock<Curation>()
        val mockMember = mock<Member>()

        // 레디스 LUA 실행 결과: 1이면 좋아요 추가, 0이면 삭제
        whenever(
            redisTemplate.execute<Any>(
                any(),
                eq(listOf(redisKey)),
                eq(redisValue)
            )
        ).thenReturn(1L) // 좋아요 추가된 상황 가정

        // 저장소 모킹
        whenever(curationRepository.findById(curationId)).thenReturn(Optional.of(mockCuration))
        whenever(memberRepository.findById(loginId)).thenReturn(Optional.of(mockMember))

        // 실행
        curationService.likeCuration(curationId, loginId)

        // 검증
        verify(curationRepository, times(1)).findById(curationId)
        verify(memberRepository, times(1)).findById(loginId)
        verify(redisTemplate, times(1)).execute<Any>(
            any(),
            eq(listOf(redisKey)),
            eq(redisValue)
        )
    }


    @Test
    @DisplayName("큐레이션 좋아요를 한 번 더 누르면 Redis에서 취소 처리가 되어야 합니다.")
    fun likeCurationWithCancel() {
        // given
        val curationId = 1L
        val loginId = 1L
        val redisKey = "curation_like:$curationId"
        val redisValue = loginId.toString()

        // Redis에 이미 좋아요가 있어서, 해당 키/값 제거됨 (0L 반환)
        whenever(
            redisTemplate.execute<Any>(
                any(),
                eq(listOf(redisKey)),
                eq(redisValue)
            )
        ).thenReturn(0L)

        val mockCuration = mock<Curation>()
        val mockMember = mock<Member>()

        whenever(curationRepository.findById(eq(curationId)))
            .thenReturn(Optional.of(mockCuration))
        whenever(memberRepository.findById(eq(loginId)))
            .thenReturn(Optional.of(mockMember))

        // when
        curationService.likeCuration(curationId, loginId)

        // then
        verify(redisTemplate, times(1)).execute<Any>(
            any(),
            eq(listOf(redisKey)),
            eq(redisValue)
        )
        verify(curationRepository, times(1)).findById(eq(curationId))
        verify(memberRepository, times(1)).findById(eq(loginId))
        verifyNoInteractions(likeRepository)
    }


    @Test
    @DisplayName("존재하지 않는 큐레이션에 좋아요를 누르면 예외가 발생해야 합니다.")
    fun likeNonExistentCuration() {
        // Mocking repository to return empty Optional (큐레이션 없음)
        whenever(curationRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // 예외 발생 검증
        AssertionsForClassTypes.assertThatThrownBy { curationService.likeCuration(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("해당 큐레이션을 찾을 수 없습니다.")

        // Verify interactions (likeRepository는 호출되지 않아야 함)
        verify(curationRepository, times(1)).findById(any<Long>())
        verify(memberRepository, never()).findById(any<Long>())
        verify(likeRepository, never()).findByCurationAndMember(any<Curation>(), any<Member>())
        verify(likeRepository, never()).save(any<Like>())
    }

    @Test
    @DisplayName("존재하지 않는 멤버가 좋아요를 누르면 예외가 발생해야 합니다.")
    fun likeByNonExistentMember() {
        // Mocking repository to return a valid Curation
        whenever(curationRepository.findById(any<Long>())).thenReturn(
            Optional.of(
                curation
            )
        )

        // Mocking repository to return empty Optional (멤버 없음)
        whenever(memberRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // 예외 발생 검증
        AssertionsForClassTypes.assertThatThrownBy { curationService.likeCuration(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("해당 멤버를 찾을 수 없습니다.")

        // Verify interactions (likeRepository는 호출되지 않아야 함)
        verify(curationRepository, times(1)).findById(any<Long>())
        verify(memberRepository, times(1)).findById(any<Long>())
        verify(likeRepository, never()).findByCurationAndMember(any<Curation>(), any<Member>())
        verify(likeRepository, never()).save(any<Like>())
    }

    @Test
    @DisplayName("큐레이션 좋아요를 Redis에서 DB로 동기화합니다.")
    fun testSyncLikesToDatabase() {
        val key = "curation_like:1"
        val keys = Set.of(key)
        val loginIds = setOf("100", "101")
        val actor = Member()
        val curation = Curation(
            title = "title",
            content = "content",
            member = actor,
        )

        val setOperations: SetOperations<String, Any> = mock<SetOperations<String, Any>>()

        whenever(redisTemplate.keys("curation_like:*")).thenReturn(keys)
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(setOperations.members(key)).thenReturn(HashSet<Any>(loginIds)) // ✅ 수정된 부분
        whenever(setOperations.size(key)).thenReturn(loginIds.size.toLong())

        whenever(curationRepository.findById(1L)).thenReturn(Optional.of(curation))
        whenever(memberRepository.findByLoginId(any<String>()))
            .thenReturn(Member())

        curationService.syncLikesToDatabase()

        verify(likeRepository, times(2)).save(any<Like>())
        verify(curationRepository, times(3)).findById(1L) // 1: 좋아요 저장용, 2: likeCount 업데이트용
        verify(curationRepository, times(1)).save(curation)
    }


    @Test
    @DisplayName("큐레이션에 대한 좋아요 여부를 Redis에서 확인합니다.")
    fun testIsLikedByMember_ReturnsTrue() {
        val curationId = 1L
        val loginId = 100L
        val key = "curation_like:$curationId"

        val setOperations: SetOperations<String, Any> = mock<SetOperations<String, Any>>()
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(redisTemplate.opsForSet().isMember(key, loginId.toString())).thenReturn(true)

        val result = curationService.isLikedByMember(curationId, loginId)

        Assertions.assertTrue(result)
    }

    @Test
    @DisplayName("팔로잉한 유저의 큐레이션을 가져옵니다.")
    fun testGetFollowingCurations() {
        val mockCurations = listOf(curation)

        whenever<List<Curation?>>(
            curationRepository.findFollowingCurations(
                eq(1L), any<Pageable>()
            )
        ).thenReturn(mockCurations)

        val result = curationService.getFollowingCurations(member, 0, 10)

        assertEquals(1, result.size)
    }


    @Test
    @DisplayName("큐레이션을 신고합니다.")
    fun testReportCuration() {
        val actor = Member()
        val curation = Curation(
            title = "title",
            content = "content",
            member = actor,
        )

        val rq = mock<Rq>()
        whenever(rq.actor)
            .thenReturn(mock<Member>())
        ReflectionTestUtils.setField(curationService, "rq", rq)

        whenever(rq.actor).thenReturn(actor)
        whenever(curationRepository.findById(1L)).thenReturn(Optional.of(curation))

        curationService.reportCuration(1L, ReportType.SPAM)

        // 여기에 report 저장 등의 로직이 추가되었다면 검증해줘야 함
        verify(reportRepository, times(1)).save(any())
    }

    @Test
    @DisplayName("조회수가 가장 높은 3개의 큐레이션을 가져옵니다.")
    fun testGetTrendingCuration_whenRedisHasData() {
        // Given

        val c1 = Curation(
            "Test Title1",
            "Test Content1",
            member
        )
        c1.viewCount = 30
        ReflectionTestUtils.setField(c1, "id", 1L)

        val c2 = Curation(
            "Test Title2",
            "Test Content2",
            member
        )
        c2.viewCount = 40
        ReflectionTestUtils.setField(c2, "id", 2L)
        val c3 = Curation(
            "Test Title3",
            "Test Content3",
            member
        )
        c3.viewCount = 50
        ReflectionTestUtils.setField(c3, "id", 3L)


        val zSetOperations: ZSetOperations<String, Any> = mock<ZSetOperations<String, Any>>()
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)

        whenever(zSetOperations.reverseRange("day_view_count:", 0, 2)).thenReturn(setOf<Any>("1", "2", "3"))

        whenever(curationRepository.findById(1L)).thenReturn(Optional.of(c1))
        whenever(curationRepository.findById(2L)).thenReturn(Optional.of(c2))
        whenever(curationRepository.findById(3L)).thenReturn(Optional.of(c3))

        whenever(zSetOperations.score("day_view_count:", "1")).thenReturn(50.0)
        whenever(zSetOperations.score("day_view_count:", "2")).thenReturn(40.0)
        whenever(zSetOperations.score("day_view_count:", "3")).thenReturn(30.0)

        // When
        val result = curationService.getTrendingCuration()

        // Then
        assertNotNull(result)
        assertEquals(3, result.curations.size)
        assertEquals(50L, result.curations[0].viewCount) // 첫 번째 아이템의 조회수 검증
    }

    @Test
    @DisplayName("조회수가 가장 높은 3개의 큐레이션을 가져올때 Redis에 데이터가 없을 경우 DB에서 가져옵니다.")
    fun testGetTrendingCuration_whenRedisIsEmpty_thenFallbackToDb() {
        // Given
        val zSetOperations: ZSetOperations<String, Any> = mock<ZSetOperations<String, Any>>()
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.reverseRange("day_view_count:", 0, 2)).thenReturn(emptySet())

        val c1 = Curation(
            "Test Title1",
            "Test Content1",
            member
        )
        c1.viewCount = 30
        ReflectionTestUtils.setField(c1, "id", 1L)

        val c2 = Curation(
            "Test Title2",
            "Test Content2",
            member
        )
        c2.viewCount = 100
        ReflectionTestUtils.setField(c2, "id", 2L)


        val c3 = Curation(
            "Test Title1",
            "Test Content1",
            member
        )
        c3.viewCount = 50
        ReflectionTestUtils.setField(c3, "id", 3L)


        whenever(curationRepository.findTop3ByOrderByViewCountDesc()).thenReturn(listOf(c1, c2, c3))

        // When
        val result = curationService.getTrendingCuration()

        // Then
        assertNotNull(result)
        assertEquals(3, result.curations.size)
        assertEquals(100, result.curations[0].viewCount)
    }
}