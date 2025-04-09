package com.team8.project2.domain.member.controller;

import java.io.IOException;
import java.util.List;

import com.team8.project2.domain.admin.service.AdminService;
import com.team8.project2.domain.comment.entity.Comment;
import com.team8.project2.domain.comment.service.CommentService;
import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.curation.service.CurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.team8.project2.domain.member.dto.AllMemberResDto;
import com.team8.project2.domain.member.dto.CuratorInfoDto;
import com.team8.project2.domain.member.dto.FollowResDto;
import com.team8.project2.domain.member.dto.FollowingResDto;
import com.team8.project2.domain.member.dto.MemberReqDTO;
import com.team8.project2.domain.member.dto.MemberResDTO;
import com.team8.project2.domain.member.dto.MemberUpdateReqDTO;
import com.team8.project2.domain.member.dto.UnfollowResDto;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.service.MemberService;
import com.team8.project2.global.Rq;
import com.team8.project2.global.dto.RsData;
import com.team8.project2.global.exception.ServiceException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MemberController {

    @Autowired
    @Lazy
    private final CurationService curationService;
    private final AdminService adminService;
    private final MemberService memberService;
    private final CommentService commentService;
    private final Rq rq;


    @PostMapping("/join")
    public RsData<LoginResBody> join(@RequestBody @Valid MemberReqDTO body) {
        memberService.findByMemberId(body.getMemberId())
                .ifPresent(member -> {
                    throw new ServiceException("409-1","사용중인 아이디");
                });

        Member member = memberService.join(body.toEntity());

        if (member.getId() == null) {
            throw new ServiceException("500-2", "회원가입 후 ID가 설정되지 않았습니다.");
        }

        String accessToken = memberService.genAccessToken(member);
        rq.addCookie("accessToken", accessToken);
        rq.addCookie("role", member.getRole().name());

        return new RsData<>(
                "201-1",
                "회원 가입이 완료되었습니다.",
                new LoginResBody(MemberResDTO.fromEntity(member), accessToken)
        );
    }
    record LoginReqBody(@NotBlank String username, @NotBlank String password) {}
    record LoginResBody(MemberResDTO item, String accessToken) {}
    @PostMapping("/login")
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody reqBody) {
        log.info("[login.reqBody.userName]:"+reqBody.username);
        Member member = memberService.findByMemberId(reqBody.username).orElseThrow(
                () -> new ServiceException("401-1", "잘못된 아이디입니다.")
        );

        if(!member.getPassword().equals(reqBody.password)) {
            throw new ServiceException("401-2", "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = memberService.genAccessToken(member);
        log.info("[accessToken]:" + accessToken);

        rq.addCookie("accessToken", accessToken);
        rq.addCookie("role", member.getRole().name());
        return new RsData<>(
                "200-1",
                "%s님 환영합니다.".formatted(member.getUsername()),
                new LoginResBody(
                        MemberResDTO.fromEntity(member),
                        accessToken
                )
        );
    }
    @GetMapping("/me")
    public RsData<MemberResDTO> getMyInfo() {
        log.info("🔍 [/me] 요청 수신됨");

        // ✅ JWT에서 사용자 정보 가져오기
        Member member = rq.getActor();

        if (member == null) {
            log.warn("⚠️ [/me] 인증된 사용자 정보 없음 (rq.getActor() == null)");
            throw new ServiceException("401-3", "유효하지 않은 인증 정보입니다.");
        }

        log.info("[/me] 사용자 인증 성공 - ID: {}, Username: {}", member.getId(), member.getUsername());

        try {
            MemberResDTO memberResDTO = MemberResDTO.fromEntity(member);
            log.info("[/me] MemberResDTO 변환 성공: {}", memberResDTO);
            return new RsData<>("200-2", "내 정보 조회 성공", memberResDTO);
        } catch (Exception e) {
            log.error("[/me] MemberResDTO 변환 중 오류 발생: ", e);
            throw new ServiceException("500-1", "사용자 정보 변환 중 오류 발생");
        }
        // return new RsData<>("200-2", "내 정보 조회 성공", MemberResDTO.fromEntity(member));
    }
    @PostMapping("/logout")
    public RsData<Void> logout() {
        rq.removeCookie("accessToken"); // JWT 삭제
        rq.removeCookie("role");

        return new RsData<>("200-3", "로그아웃 되었습니다.");
    }

    @GetMapping("/{username}")
    public RsData<CuratorInfoDto> getCuratorInfo(@PathVariable String username) {
        CuratorInfoDto curatorInfoDto = memberService.getCuratorInfo(username);
        return new RsData<>("200-4", "큐레이터 정보 조회 성공", curatorInfoDto);
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("isAuthenticated()")
    public RsData<MemberResDTO> updateMember(
            @PathVariable String memberId,
            @RequestBody @Valid MemberUpdateReqDTO updateReqDTO) {

        Member actor = rq.getActor();

        if (actor == null || !actor.getMemberId().equals(memberId)) {
            throw new ServiceException("403-1", "권한이 없습니다.");
        }

        Member existingMember = memberService.findByMemberId(memberId).get();

        if (updateReqDTO.getEmail() != null) {
            existingMember.setEmail(updateReqDTO.getEmail());
        }
        if (updateReqDTO.getUsername() != null) {
            existingMember.setUsername(updateReqDTO.getUsername());
        }
        if (updateReqDTO.getProfileImage() != null) {
            existingMember.setProfileImage(updateReqDTO.getProfileImage());
        }
        if (updateReqDTO.getIntroduce() != null) {
            existingMember.setIntroduce(updateReqDTO.getIntroduce());
        }

        Member updatedMember = memberService.updateMember(existingMember);
        return new RsData<>("200-5", "회원 정보가 수정되었습니다.", MemberResDTO.fromEntity(updatedMember));
    }


    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public RsData<FollowResDto> follow(@PathVariable String username) {
        Member actor = rq.getActor();
        FollowResDto followResDto = memberService.followUser(actor, username);
        return new RsData<>("200-1", "%s님을 팔로우했습니다.".formatted(username), followResDto);
    }

    @PostMapping("/{username}/unfollow")
    @PreAuthorize("isAuthenticated()")
    public RsData<UnfollowResDto> unfollow(@PathVariable String username) {
        Member actor = rq.getActor();
        UnfollowResDto unfollowResDto = memberService.unfollowUser(actor, username);
        return new RsData<>("200-1", "%s님을 팔로우 취소했습니다.".formatted(username), unfollowResDto);
    }

    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()")
    public RsData<FollowingResDto> following() {
        Member actor = rq.getActor();
        FollowingResDto followingResDto = memberService.getFollowingUsers(actor);
        return new RsData<>("200-1", "팔로우 중인 사용자를 조회했습니다.", followingResDto);
    }

    @PostMapping("/profile/images/upload")
    @PreAuthorize("isAuthenticated()")
    public RsData<Void> updateProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            memberService.updateProfileImage(file);
        } catch (IOException e) {
            return new RsData<>("500-1", "프로필 이미지 업로드에 실패했습니다.");
		}
		return new RsData<>("200-1", "프로필 이미지가 변경되었습니다.");
    }

    @GetMapping("/members")
    @PreAuthorize("isAuthenticated()")
    public RsData<AllMemberResDto> findAllMember(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size)
    {
        Member member = rq.getActor();
        if (!member.isAdmin()) {
            return new RsData<>("403-1", "관리자 권한이 없습니다.");
        }
        AllMemberResDto allMemberResDto = adminService.getAllMembers(page, size);
        return RsData.success("멤버 조회 성공", allMemberResDto);
    }

    @DeleteMapping("/delete")
    public RsData<Void> deleteMember() {
        Member actor = rq.getActor();
        List<Curation> curations = curationService.findAllByMember(actor);
        List<Comment> comments = commentService.findAllByAuthor(actor);
        adminService.deleteMember(actor);
        return new RsData<>("200-6", "회원 탈퇴가 완료되었습니다.");
    }
}
