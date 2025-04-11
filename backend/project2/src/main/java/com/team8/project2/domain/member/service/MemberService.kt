package com.team8.project2.domain.member.service

import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.image.service.S3Uploader
import com.team8.project2.domain.member.dto.CuratorInfoDto
import com.team8.project2.domain.member.dto.FollowResDto
import com.team8.project2.domain.member.dto.FollowingResDto
import com.team8.project2.domain.member.dto.UnfollowResDto
import com.team8.project2.domain.member.entity.Follow
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.event.ProfileImageUpdateEvent
import com.team8.project2.domain.member.repository.FollowRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.member.repository.findByIdOrThrow
import com.team8.project2.global.Rq
import com.team8.project2.global.exception.ServiceException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val authTokenService: AuthTokenService,
    private val followRepository: FollowRepository,
    private val rq: Rq,
    private val curationRepository: CurationRepository,
    private val s3Uploader: S3Uploader,
    private val eventPublisher: ApplicationEventPublisher

) {
    fun join(memberId: String?, password: String?, role: RoleEnum?, email: String?, profileImage: String?): Member {
        return join(memberId, password, role, email, profileImage, null)
    }

    fun count(): Long {
        return memberRepository.count()
    }

    @Transactional
    fun join(
        memberId: String?, password: String?, role: RoleEnum?, email: String?, profileImage: String?,
        introduce: String?
    ): Member {
        var role = role
        if (role == null) {
            role = RoleEnum.MEMBER
        }
        val member = Member(
            memberId = memberId,
            password = password,
            role = RoleEnum.MEMBER,  // 기본값 지정이 사라졌을 수 있으므로 명시
            email = email,
            profileImage = profileImage,
            introduce = introduce
        )
        return memberRepository.save(member)
    }

    @Transactional
    fun join(member: Member): Member {
        return memberRepository.save(member)
    }

    fun findByMemberId(memberId: String): Member? {
        return memberRepository.findByMemberId(memberId)
    }

    fun findByUsername(username: String?): Member? {
        return memberRepository.findByUsername(username)
    }

    fun findById(id: Long?): Member {
        return memberRepository.findByIdOrThrow(id)
    }

    fun getAuthToken(member: Member?): String {
        return authTokenService.genAccessToken(member!!)
    }

    @Transactional
    fun deleteMember(memberId: String?) {
        val member = memberRepository.findByMemberId(memberId)
            ?: throw ServiceException("404-1", "해당 회원을 찾을 수 없습니다.")
        memberRepository.delete(member)
    }

    @Transactional
    fun getMemberByAccessToken(accessToken: String?): Optional<Member> {
        val payload = authTokenService.getPayload(accessToken)
        if (payload == null) {
            return Optional.empty()
        }

        val id = payload["id"] as Long
        val memberId = payload["memberId"] as String?

        return Optional.of(Member(id, memberId!!))
    }

    fun genAccessToken(member: Member?): String {
        return authTokenService.genAccessToken(member!!)
    }

    @Transactional
    fun followUser(follower: Member, username: String?): FollowResDto {
        val followee = findByUsername(username)
            ?: throw ServiceException("404-1", "존재하지 않는 사용자입니다.")

        if (follower.getMemberId() == followee.getMemberId()) {
            throw ServiceException("400-1", "자신을 팔로우할 수 없습니다.")
        }

        var follow = Follow()
        follow.setFollowerAndFollowee(follower, followee)

        followRepository.findByFollowerAndFollowee(follower, followee)?.let {
            throw ServiceException("400-1", "이미 팔로우중인 사용자입니다.")
        }

        follow = followRepository.save(follow)
        return FollowResDto.fromEntity(follow)
    }

    @Transactional
    fun unfollowUser(follower: Member, followeeId: String?): UnfollowResDto {
        val followee = findByUsername(followeeId)

            ?: throw ServiceException("404-1", "존재하지 않는 사용자입니다.")

        if (follower.getMemberId() == followee.getMemberId()) {
            throw ServiceException("400-1", "자신을 팔로우할 수 없습니다.")
        }

        val follow = followRepository.findByFollowerAndFollowee(follower, followee)
            ?: throw ServiceException("400-1", "팔로우중이 아닙니다.")

        followRepository.delete(follow)
        return UnfollowResDto.fromEntity(follow)
    }

    @Transactional(readOnly = true)
    fun getFollowingUsers(actor: Member?): FollowingResDto {
        val followings = followRepository.findByFollower(actor!!)
            ?.sortedByDescending { it?.followedAt }
        return FollowingResDto.fromEntity(followings!!)
    }

    @Transactional
    fun updateMember(member: Member): Member {
        return memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun isFollowed(followeeId: Long?, followerId: Long?): Boolean {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId!!, followeeId!!)
    }

    @Transactional(readOnly = true)
    fun getCuratorInfo(username: String?): CuratorInfoDto {
        val member = memberRepository.findByUsername(username)
            ?: throw ServiceException("404-1", "해당 큐레이터를 찾을 수 없습니다.")

        val curationCount = curationRepository.countByMember(member)
        var isLogin = false
        var isFollowed = false
        if (rq.isLogin) {
            isLogin = true
            val actor = rq.actor
            isFollowed = followRepository.existsByFollowerIdAndFolloweeId(actor.id!!, member.id!!)
        }

        return CuratorInfoDto(
            username.toString(), member.profileImage.toString(), member.introduce.toString(), curationCount, isFollowed,
            isLogin
        )
    }

    @Transactional
    @Throws(IOException::class)
    fun updateProfileImage(imageFile: MultipartFile?) {
        val actor = rq.actor
        val imageFileName = s3Uploader.uploadFile(imageFile!!)
        val oldProfileImageUrl = actor.profileImage
        actor.profileImage = s3Uploader.baseUrl + imageFileName

        memberRepository.save(actor)
        eventPublisher.publishEvent(ProfileImageUpdateEvent(oldProfileImageUrl!!))
    }
}
