package com.team8.project2.domain.link.service

import com.team8.project2.domain.link.dto.LinkReqDTO
import com.team8.project2.domain.link.entity.Link
import com.team8.project2.domain.link.repository.LinkRepository
import com.team8.project2.global.exception.ServiceException
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.*

@ExtendWith(MockitoExtension::class)
class LinkServiceTest {

    @Mock
    lateinit var linkRepository: LinkRepository

    @Mock
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    lateinit var request: HttpServletRequest

    @Mock
    lateinit var linkClickService: LinkClickService

    @InjectMocks
    lateinit var linkService: LinkService

    private lateinit var link: Link

    @BeforeEach
    fun setUp() {
        link = Link.builder()
            .url("https://example.com")
            .click(0)
            .build()
    }

    @Test
    @DisplayName("링크를 추가할 수 있다")
    fun addLink() {
        val dto = LinkReqDTO("https://example.com", "테스트 제목", "테스트 설명")
        whenever(linkRepository.save(any())).thenReturn(link)

        val created = linkService.addLink(dto)

        Assertions.assertNotNull(created)
        Assertions.assertEquals(dto.url, created.url)
        verify(linkRepository).save(any())
    }

    @Test
    @DisplayName("링크를 수정할 수 있다")
    fun updateLink() {
        val newUrl = "https://updated-example.com"
        whenever(linkRepository.findById(1L)).thenReturn(Optional.of(link))
        whenever(linkRepository.save(any())).thenReturn(link)

        val updated = linkService.updateLink(1L, newUrl)

        Assertions.assertEquals(newUrl, updated.url)
        verify(linkRepository).save(any())
    }

    @Test
    @DisplayName("링크 수정 시 링크가 존재하지 않으면 예외가 발생한다")
    fun updateLinkNotFound() {
        whenever(linkRepository.findById(1L)).thenReturn(Optional.empty())

        val ex = Assertions.assertThrows(ServiceException::class.java) {
            linkService.updateLink(1L, "https://updated.com")
        }

        Assertions.assertEquals("404-1", ex.code)
        Assertions.assertEquals("해당 링크를 찾을 수 없습니다.", ex.message)
    }

    @Test
    @DisplayName("링크를 삭제할 수 있다")
    fun deleteLink() {
        whenever(linkRepository.findById(1L)).thenReturn(Optional.of(link))

        linkService.deleteLink(1L)

        verify(linkRepository).delete(any())
    }

    @Test
    @DisplayName("링크 삭제 시 링크가 존재하지 않으면 예외가 발생한다")
    fun deleteLinkNotFound() {
        whenever(linkRepository.findById(1L)).thenReturn(Optional.empty())

        val ex = Assertions.assertThrows(ServiceException::class.java) {
            linkService.deleteLink(1L)
        }

        Assertions.assertEquals("404-1", ex.code)
        Assertions.assertEquals("해당 링크를 찾을 수 없습니다.", ex.message)
    }

    @Test
    @DisplayName("링크가 존재하지 않으면 생성해서 반환한다")
    fun getLinkOrCreate() {
        val url = "https://example.com"
        whenever(linkRepository.findByUrl(url)).thenReturn(null)
        whenever(linkRepository.save(any())).thenReturn(link)

        val found = linkService.getLink(url)

        Assertions.assertEquals(url, found.url)
        verify(linkRepository).save(any())
    }

    @Test
    @DisplayName("링크가 존재하면 기존 링크를 반환한다")
    fun getExistingLink() {
        val url = "https://example.com"
        whenever(linkRepository.findByUrl(url)).thenReturn(link)

        val found = linkService.getLink(url)

        Assertions.assertEquals(url, found.url)
        verify(linkRepository, never()).save(any())
    }

    @Test
    @DisplayName("링크 클릭 수는 한 번만 증가해야 한다")
    fun increaseClickOnce() {
        val valueOps: ValueOperations<String, String> = mock()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(request.remoteAddr).thenReturn("192.168.0.1")
        whenever(valueOps.setIfAbsent(any(), eq("true"), eq(Duration.ofMinutes(10))))
            .thenReturn(true)
            .thenReturn(false)

        whenever(linkRepository.findById(1L)).thenReturn(Optional.of(link))

        repeat(3) { linkService.getLinkAndIncrementClick(1L, request) }

        verify(linkClickService, times(1)).increaseClickCount(any())
    }

    @Test
    @DisplayName("이미 클릭한 경우 클릭 수는 증가하지 않는다")
    fun doNotIncreaseIfAlreadyClicked() {
        val valueOps: ValueOperations<String, String> = mock()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(request.remoteAddr).thenReturn("192.168.0.1")
        whenever(valueOps.setIfAbsent(any(), eq("true"), eq(Duration.ofMinutes(10)))).thenReturn(false)
        whenever(linkRepository.findById(1L)).thenReturn(Optional.of(link))

        val before = link.click
        linkService.getLinkAndIncrementClick(1L, request)

        Assertions.assertEquals(before, link.click)
        verify(linkClickService, never()).increaseClickCount(any())
    }

    @Test
    @DisplayName("링크 조회 시 존재하지 않으면 예외가 발생한다")
    fun getLinkThrowsIfNotFound() {
        val valueOps: ValueOperations<String, String> = mock()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(request.remoteAddr).thenReturn("192.168.0.1")
        whenever(valueOps.setIfAbsent(any(), eq("true"), eq(Duration.ofMinutes(10)))).thenReturn(true)
        whenever(linkRepository.findById(1L)).thenReturn(Optional.empty())

        val ex = Assertions.assertThrows(ServiceException::class.java) {
            linkService.getLinkAndIncrementClick(1L, request)
        }

        Assertions.assertEquals("404-1", ex.code)
        Assertions.assertEquals("해당 링크를 찾을 수 없습니다.", ex.message)
    }
}
