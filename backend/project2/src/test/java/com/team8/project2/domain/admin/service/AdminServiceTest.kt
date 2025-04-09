package com.team8.project2.domain.admin.service

import com.team8.project2.domain.admin.dto.StatsResDto
import com.team8.project2.domain.comment.repository.CommentRepository
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.member.repository.FollowRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.domain.playlist.repository.PlaylistRepository
import com.team8.project2.global.exception.NotFoundException
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminService 테스트")
class AdminServiceTest {

    @Mock
    lateinit var curationRepository: CurationRepository
    @Mock
    lateinit var memberRepository: MemberRepository
    @Mock
    lateinit var playlistRepository: PlaylistRepository
    @Mock
    lateinit var memberService: MemberService
    @Mock
    lateinit var curationService: CurationService
    @Mock
    lateinit var entityManager: EntityManager
    @Mock
    lateinit var commentService: CommentService
    @Mock
    lateinit var followRepository: FollowRepository
    @Mock
    lateinit var commentRepository: CommentRepository
    @Mock
    lateinit var reportRepository: ReportRepository

    @InjectMocks
    lateinit var adminService: AdminService

    @Test
    @DisplayName("큐레이션 삭제 - 존재하는 큐레이션이면 정상적으로 삭제된다.")
    fun deleteCuration_ShouldDelete_WhenCurationExists() {
        val curationId = 1L
        `when`(curationRepository.existsById(curationId)).thenReturn(true)

        adminService.deleteCuration(curationId)

        verify(curationRepository, times(1)).deleteById(curationId)
    }

    @Test
    @DisplayName("큐레이션 삭제 - 존재하지 않는 큐레이션이면 NotFoundException 발생")
    fun deleteCuration_ShouldThrowException_WhenCurationNotFound() {
        val curationId = 1L
        `when`(curationRepository.existsById(curationId)).thenReturn(false)

        val exception = assertFailsWith<NotFoundException> {
            adminService.deleteCuration(curationId)
        }

        assertEquals("큐레이션을 찾을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("멤버 삭제 - 존재하는 멤버이면 정상적으로 삭제된다.")
    fun deleteMember_ShouldDelete_WhenMemberExists() {
        val memberId = 1L
        `when`(memberRepository.existsById(memberId)).thenReturn(true)

        adminService.deleteMemberById(memberId)

        verify(memberRepository, times(1)).deleteById(memberId)
    }

    @Test
    @DisplayName("멤버 삭제 - 존재하지 않는 멤버이면 NotFoundException 발생")
    fun deleteMember_ShouldThrowException_WhenMemberNotFound() {
        val memberId = 1L
        `when`(memberRepository.existsById(memberId)).thenReturn(false)

        val exception = assertFailsWith<NotFoundException> {
            adminService.deleteMemberById(memberId)
        }

        assertEquals("멤버를 찾을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("통계 조회 - 큐레이션 및 플레이리스트의 조회수와 좋아요 수를 올바르게 반환한다.")
    fun getCurationAndPlaylistStats_ShouldReturnStatsResDto() {
        `when`(curationRepository.sumTotalViews()).thenReturn(100L)
        `when`(curationRepository.sumTotalLikes()).thenReturn(50L)
        `when`(playlistRepository.sumTotalViews()).thenReturn(200L)
        `when`(playlistRepository.sumTotalLikes()).thenReturn(80L)

        val stats: StatsResDto = adminService.getCurationAndPlaylistStats()

        assertEquals(100L, stats.totalCurationViews)
        assertEquals(50L, stats.totalCurationLikes)
        assertEquals(200L, stats.totalPlaylistViews)
        assertEquals(80L, stats.totalPlaylistLikes)
    }
}
