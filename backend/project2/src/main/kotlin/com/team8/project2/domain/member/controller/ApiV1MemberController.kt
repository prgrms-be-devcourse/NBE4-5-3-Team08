package com.team8.project2.domain.member.controller

import com.team8.project2.domain.admin.service.AdminService
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.member.dto.*
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import com.team8.project2.global.dto.RsData
import com.team8.project2.global.exception.ServiceException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
class ApiV1MemberController(
    private val curationService: CurationService,
    private val adminService: AdminService,
    private val memberService: MemberService,
    private val commentService: CommentService,
    private val rq: Rq
) {
/*    @Autowired
    @Lazy*/

    @PostMapping("/join")
    fun join(@Valid @RequestBody body:  MemberReqDTO): RsData<LoginResBody> {
        memberService.findByLoginId(body.loginId!!)?.let {
            throw ServiceException("409-1", "사용중인 아이디")
        }

        val member = memberService.join(body.toEntity())

        val accessToken = memberService.genAccessToken(member)
        rq.addCookie("accessToken", accessToken)
        rq.addCookie("role", member.role.name)

        return RsData(
            "201-1",
            "회원 가입이 완료되었습니다.",
            LoginResBody(MemberResDTO.fromEntity(member), accessToken)
        )
    }

    data class LoginReqBody(val username: @NotBlank String?, val password: @NotBlank String?)

    data class LoginResBody(val item: MemberResDTO, val accessToken: String)

    @PostMapping("/login")
    fun login(@Valid @RequestBody reqBody: LoginReqBody): RsData<LoginResBody> {
        val member = memberService.findByLoginId(reqBody.username!!)
            ?: throw ServiceException("401-1", "잘못된 아이디입니다.")

        if (member.getPassword() != reqBody.password) {
            throw ServiceException("401-2", "비밀번호가 일치하지 않습니다.")
        }

        val accessToken = memberService.genAccessToken(member)

        rq.addCookie("accessToken", accessToken)
        rq.addCookie("role", member.role.name)
        return RsData(
            "200-1",
            "${member.getUsername()}님 환영합니다.",
            LoginResBody(
                MemberResDTO.fromEntity(member),
                accessToken
            )
        )
    }

    @GetMapping("/me")
    fun getMyInfo(): RsData<MemberResDTO> {
        // ✅ JWT에서 사용자 정보 가져오기
        val member = rq.actor

        if (member == null) {
            throw ServiceException("401-3", "유효하지 않은 인증 정보입니다.")
        }

        return try {
            val memberResDTO = MemberResDTO.fromEntity(member)
            RsData("200-2", "내 정보 조회 성공", memberResDTO)
        } catch (e: Exception) {
            throw ServiceException("500-1", "사용자 정보 변환 중 오류 발생")
        }
    }

    @PostMapping("/logout")
    fun logout(): RsData<Void> {
        rq.removeCookie("accessToken") // JWT 삭제
        rq.removeCookie("role")

        return RsData("200-3", "로그아웃 되었습니다.")
    }

    @GetMapping("/{username}")
    fun getCuratorInfo(@PathVariable username: String): RsData<CuratorInfoDto> {
        val curatorInfoDto = memberService.getCuratorInfo(username)
        return RsData("200-4", "큐레이터 정보 조회 성공", curatorInfoDto)
    }

    @PutMapping("/{loginId}")
    @PreAuthorize("isAuthenticated()")
    fun updateMember(
        @PathVariable loginId: String,
        @RequestBody @Valid updateReqDTO: MemberUpdateReqDTO
    ): RsData<MemberResDTO> {
        val actor = rq.actor

        if (actor == null || actor.getLoginId() != loginId) {
            throw ServiceException("403-1", "권한이 없습니다.")
        }

        val existingMember = memberService.findByLoginId(loginId)

        if (updateReqDTO!!.email != null) {
            existingMember?.email = updateReqDTO.email
        }
        if (updateReqDTO.username != null) {
            existingMember?.setUsername(updateReqDTO.username)
        }
        if (updateReqDTO.profileImage != null) {
            existingMember?.profileImage = updateReqDTO.profileImage
        }
        if (updateReqDTO.introduce != null) {
            existingMember?.introduce = updateReqDTO.introduce
        }

        val updatedMember = memberService.updateMember(existingMember!!)
        return RsData("200-5", "회원 정보가 수정되었습니다.", MemberResDTO.fromEntity(updatedMember))
    }


    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    fun follow(@PathVariable username: String): RsData<FollowResDto> {
        val actor = rq.actor
        val followResDto = memberService.followUser(actor, username)
        return RsData("200-1", "${username}님을 팔로우했습니다.", followResDto)
    }

    @PostMapping("/{username}/unfollow")
    @PreAuthorize("isAuthenticated()")
    fun unfollow(@PathVariable username: String): RsData<UnfollowResDto> {
        val actor = rq.actor
        val unfollowResDto = memberService.unfollowUser(actor, username)
        return RsData("200-1", "${username}님을 팔로우 취소했습니다.", unfollowResDto)
    }

    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()")
    fun following(): RsData<FollowingResDto> {
        val actor = rq.actor
        val followingResDto = memberService.getFollowingUsers(actor)
        return RsData("200-1", "팔로우 중인 사용자를 조회했습니다.", followingResDto)
    }

    @PostMapping("/profile/images/upload")
    @PreAuthorize("isAuthenticated()")
    fun updateProfileImage(@RequestParam("file") file: MultipartFile?): RsData<Void> {
        try {
            memberService.updateProfileImage(file)
        } catch (e: IOException) {
            return RsData("500-1", "프로필 이미지 업로드에 실패했습니다.")
        }
        return RsData("200-1", "프로필 이미지가 변경되었습니다.")
    }

    @GetMapping("/members")
    @PreAuthorize("isAuthenticated()")
    fun findAllMember(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): RsData<AllMemberResDto> {
        val member = rq.actor
        if (!member.isAdmin) {
            return RsData("403-1", "관리자 권한이 없습니다.")
        }
        val allMemberResDto = adminService.getAllMembers(page, size)
        return RsData.success("멤버 조회 성공", allMemberResDto)
    }

    @DeleteMapping("/delete")
    fun deleteMember(): RsData<Void> {
        val actor = rq.actor
        curationService.findAllByMember(actor)
        commentService.findAllByAuthor(actor)
        adminService.deleteMember(actor)
        return RsData("200-6", "회원 탈퇴가 완료되었습니다.")
    }
}
