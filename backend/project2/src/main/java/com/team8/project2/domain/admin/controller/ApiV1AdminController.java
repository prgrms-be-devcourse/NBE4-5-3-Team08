package com.team8.project2.domain.admin.controller;

import java.util.List;

import com.team8.project2.domain.curation.curation.service.CurationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team8.project2.domain.admin.dto.StatsResDto;
import com.team8.project2.domain.admin.service.AdminService;
import com.team8.project2.domain.comment.service.CommentService;
import com.team8.project2.domain.curation.report.dto.ReportedCurationsDetailResDto;
import com.team8.project2.domain.curation.report.service.ReportService;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.service.MemberService;
import com.team8.project2.global.Rq;
import com.team8.project2.global.dto.RsData;
import com.team8.project2.global.exception.ServiceException;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ApiV1AdminController {

	private final CurationService curationService;
	private final AdminService adminService;
	private final Rq rq;
	private final MemberService memberService;
	private final CommentService commentService;
	private final ReportService reportService;

	// ✅ 큐레이션 삭제
	@DeleteMapping("/curations/{curationId}")
	public RsData<String> deleteCuration(@PathVariable Long curationId) {
		Member member = rq.getActor();

		curationService.deleteCuration(curationId, member);
		return new RsData<>("204-1", "글이 성공적으로 삭제되었습니다.", null);
	}

	// ✅ 멤버 삭제
	@DeleteMapping("/members/{memberId}")
	public RsData<Unit> deleteMember(@PathVariable Long memberId) {
		Member member = memberService.findById(memberId)
			.orElseThrow(() -> new ServiceException("404-1", "해당 회원을 찾을 수 없습니다."));
		adminService.deleteMember(member);
		return RsData.success("멤버가 삭제되었습니다.");
	}

	// ✅ 일정 개수 이상 신고된 큐레이션 조회
	@GetMapping("/reported-curations")
	public RsData<List<Long>> getReportedCurations(@RequestParam(defaultValue = "5") int minReports,
		@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return RsData.success("신고된 큐레이션 목록 조회 성공", adminService.getReportedCurations(minReports, page, size));
	}

	// ✅ 일정 개수 이상 신고된 큐레이션 상세 조회
	@GetMapping("/reported-curations-detail")
	public RsData<List<ReportedCurationsDetailResDto>> getReportedCurationsDetail(
		@RequestParam(defaultValue = "5") int minReports, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		List<Long> reportedcurations = adminService.getReportedCurations(minReports, page, size);
		reportedcurations.forEach(System.out::println);
		return RsData.success("신고된 큐레이션 목록 조회 성공", reportService.getReportedCurationsDetailResDtos(reportedcurations));
	}

	// ✅ 큐레이션 & 플레이리스트 통계 조회
	@GetMapping("/stats")
	public RsData<StatsResDto> getStats() {
		return RsData.success("트래픽 통계 조회 성공", adminService.getCurationAndPlaylistStats());
	}
}
