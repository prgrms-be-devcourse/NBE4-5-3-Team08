package com.team8.project2.domain.admin.service

import com.team8.project2.domain.admin.dto.StatsResDto
import com.team8.project2.domain.comment.repository.CommentRepository
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.curation.report.repository.ReportRepository
import com.team8.project2.domain.member.dto.AllMemberResDto
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.repository.FollowRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.domain.playlist.repository.PlaylistRepository
import com.team8.project2.global.exception.NotFoundException
import com.team8.project2.global.exception.ServiceException
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
	private val curationRepository: CurationRepository,
	private val memberRepository: MemberRepository,
	private val playlistRepository: PlaylistRepository,
	private val memberService: MemberService,
	private val curationService: CurationService,
	private val entityManager: EntityManager,
	private val commentService: CommentService,
	private val followRepository: FollowRepository,
	private val commentRepository: CommentRepository,
	private val reportRepository: ReportRepository
) {

	/**
	 * ✅ 회원 탈퇴 시 관련 데이터 일괄 삭제 처리
	 */
	@Transactional(noRollbackFor = [ServiceException::class])
	fun deleteMember(member: Member) {
		commentRepository.deleteByAuthor(member)
		curationRepository.deleteByMember(member)
		followRepository.deleteByFollowerOrFollowee(member, member)

		// ✅ Lombok의 @Getter에 의해 Kotlin에서는 프로퍼티 접근으로 가능
		memberService.deleteMember(member.memberId)
	}

	/**
	 * ✅ ID 기반 회원 삭제
	 */
	fun deleteMemberById(id: Long) {
		if (!memberRepository.existsById(id)) {
			throw NotFoundException("멤버를 찾을 수 없습니다.")
		}
		memberRepository.deleteById(id)
	}

	/**
	 * ✅ 큐레이션 삭제
	 */
	@Transactional
	fun deleteCuration(curationId: Long) {
		if (!curationRepository.existsById(curationId)) {
			throw NotFoundException("큐레이션을 찾을 수 없습니다.")
		}
		curationRepository.deleteById(curationId)
	}

	/**
	 * ✅ 최소 신고 횟수를 기준으로 신고된 큐레이션 ID 리스트 조회
	 */
	fun getReportedCurations(minReports: Int, page: Int, size: Int): List<Long> {
		val pageable: Pageable = PageRequest.of(page, size, Sort.by("createdAt"))
		val reportedCurations: List<Curation> = curationRepository.findReportedCurations(minReports, pageable)

		return reportedCurations.map { it.id!! }
	}

	/**
	 * ✅ 큐레이션 및 플레이리스트 통계 조회
	 */
	fun getCurationAndPlaylistStats(): StatsResDto {
		val totalCurationViews = curationRepository.sumTotalViews()
		val totalCurationLikes = curationRepository.sumTotalLikes()
		val totalPlaylistViews = playlistRepository.sumTotalViews()
		val totalPlaylistLikes = playlistRepository.sumTotalLikes()

		return StatsResDto(
			totalCurationViews,
			totalCurationLikes,
			totalPlaylistViews,
			totalPlaylistLikes
		)
	}

	/**
	 * ✅ 전체 회원 목록 조회 (페이징 포함)
	 */
	fun getAllMembers(page: Int, size: Int): AllMemberResDto {
		val pageable = PageRequest.of(page, size, Sort.by("createdDate").descending())
		val memberPage = memberRepository.findAll(pageable)

		return AllMemberResDto.of(
			memberPage.content,
			memberPage.totalPages,
			memberPage.totalElements,
			memberPage.numberOfElements,
			memberPage.size
		)
	}
}
