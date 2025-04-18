package com.team8.project2.global.init

import com.team8.project2.domain.comment.dto.CommentDto
import com.team8.project2.domain.comment.service.CommentService
import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.curation.repository.CurationRepository
import com.team8.project2.domain.curation.curation.service.CurationService
import com.team8.project2.domain.image.service.S3Uploader
import com.team8.project2.domain.member.entity.Follow
import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.entity.RoleEnum
import com.team8.project2.domain.member.repository.FollowRepository
import com.team8.project2.domain.member.repository.MemberRepository
import com.team8.project2.domain.playlist.dto.PlaylistCreateDto
import com.team8.project2.domain.playlist.entity.PlaylistItem
import com.team8.project2.domain.playlist.repository.PlaylistItemRepository
import com.team8.project2.domain.playlist.repository.PlaylistRepository
import com.team8.project2.domain.playlist.service.PlaylistService
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BaseInitData(
    private val memberRepository: MemberRepository,
    private val curationRepository: CurationRepository,
    private val playlistRepository: PlaylistRepository,
    private val curationService: CurationService,
    private val s3Uploader: S3Uploader,
    private val playlistItemRepository: PlaylistItemRepository,
) {

    @Bean
    fun init(commentService: CommentService, followRepository: FollowRepository, playlistService: PlaylistService) = ApplicationRunner {
        if (memberRepository.count() == 0L && curationRepository.count() == 0L) {
            val members = createMembers()
            createFollowRelations(members, followRepository)
            createCurationData(members, curationService, commentService)
            createPlaylistData(members, playlistService)
        }
    }

    private fun createMembers(): List<Member> {
        val members = mutableListOf<Member>()
        members.add(createMember("team8@gmail.com", "team8", "loginId", "username", "password", s3Uploader.baseUrl + "default-profile.svg", "test", RoleEnum.MEMBER))
        members.add(createMember("team9@gmail.com", "team9", "othermember", "other", "password", s3Uploader.baseUrl + "default-profile.svg", "test2", RoleEnum.MEMBER))
        members.add(createMember("team10@gmail.com", "team10", "othermember2", "other2", "password", s3Uploader.baseUrl + "default-profile.svg", "test3", RoleEnum.MEMBER))
        members.add(createMember("admin@gmail.com", "admin", "admin", "admin", "password", s3Uploader.baseUrl + "default-profile.svg", "admin", RoleEnum.ADMIN))
        members.add(createMember("team11@gmail.com", "team11", "member11", "username11", "password", s3Uploader.baseUrl + "default-profile.svg", "test11", RoleEnum.MEMBER))
        members.add(createMember("team12@gmail.com", "team12", "member12", "username12", "password", s3Uploader.baseUrl + "default-profile.svg", "test12", RoleEnum.MEMBER))
        members.add(createMember("team13@gmail.com", "team13", "member13", "username13", "password", s3Uploader.baseUrl + "default-profile.svg", "test13", RoleEnum.MEMBER))
        members.add(createMember("team14@gmail.com", "team14", "member14", "username14", "password", s3Uploader.baseUrl + "default-profile.svg", "test14", RoleEnum.MEMBER))
        members.add(createMember("team15@gmail.com", "team15", "member15", "username15", "password", s3Uploader.baseUrl + "default-profile.svg", "test15", RoleEnum.MEMBER))
        members.add(createMember("team16@gmail.com", "team16", "member16", "username16", "password", s3Uploader.baseUrl + "default-profile.svg", "test16", RoleEnum.MEMBER))
        members.add(createMember("team17@gmail.com", "team17", "member17", "username17", "password", s3Uploader.baseUrl + "default-profile.svg", "test17", RoleEnum.MEMBER))
        members.add(createMember("team18@gmail.com", "team18", "member18", "username18", "password", s3Uploader.baseUrl + "default-profile.svg", "test18", RoleEnum.MEMBER))
        members.add(createMember("team19@gmail.com", "team19", "member19", "username19", "password", s3Uploader.baseUrl + "default-profile.svg", "test19", RoleEnum.MEMBER))
        members.add(createMember("team20@gmail.com", "team20", "member20", "username20", "password", s3Uploader.baseUrl + "default-profile.svg", "test20", RoleEnum.MEMBER))
        for (i in 21..50) {
            members.add(createMember("team$i@gmail.com", "team$i", "member$i", "username$i", "password", s3Uploader.baseUrl + "default-profile.svg", "test$i", RoleEnum.MEMBER))
        }
        return members
    }

    private fun createMember(email: String, username: String, loginId: String, displayName: String, password: String, profileImage: String, introduce: String, role: RoleEnum): Member {
        val member = Member(
            loginId = loginId,
            username = displayName,
            password = password,
            role = role,
            profileImage = profileImage,
            email = email,
            introduce = introduce
        )

        return memberRepository.save(member)
    }

    private fun createFollowRelations(members: List<Member>, followRepository: FollowRepository) {
        followRepository.save(Follow().apply { setFollowerAndFollowee(members[2], members[0]) })
        followRepository.save(Follow().apply { setFollowerAndFollowee(members[2], members[1]) })
    }

    private fun createCurationData(members: List<Member>, curationService: CurationService, commentService: CommentService) {
        val titles = listOf(
            "최신 개발 트렌드", "웹 디자인 팁", "프로그래밍 언어 비교", "AI와 머신러닝의 미래", "클라우드 서비스 비교",
            "효율적인 팀워크 구축법", "DevOps와 CI/CD 도입 방법", "자바스크립트 프레임워크 비교", "Python으로 데이터 분석 시작하기", "리팩토링과 유지보수"
        )
        val contents = listOf(
            "최신 개발 트렌드에 대해 알아보며, 빠르게 변화하는 기술에 대응하는 방법을 다룹니다.",
            "웹 디자인에서 중요한 요소와 최신 디자인 트렌드에 대해 소개합니다.",
            "가장 많이 사용되는 프로그래밍 언어들의 특징과 차이점을 비교해봅니다.",
            "AI와 머신러닝의 발전과 향후 전망을 다룬 기사입니다.",
            "AWS, GCP, Azure 등 주요 클라우드 서비스의 비교와 장단점을 설명합니다.",
            "효율적인 팀워크를 만들기 위한 전략과 도구를 공유합니다.",
            "DevOps와 CI/CD의 개념을 소개하고 이를 어떻게 도입할 수 있는지에 대해 설명합니다.",
            "React, Vue, Angular 등 주요 자바스크립트 프레임워크들을 비교하고 선택하는 방법을 알려줍니다.",
            "Python을 활용한 데이터 분석의 기본부터 고급까지의 팁과 기술을 다룹니다.",
            "리팩토링을 통한 코드 품질 개선과 유지보수 방법에 대해 알아봅니다."
        )
        val links = listOf("https://www.naver.com/", "https://www.github.com/", "https://www.stackoverflow.com/", "https://www.medium.com/")
        val tags = listOf("개발", "프로그래밍", "웹", "디자인", "기술", "클라우드", "팀워크", "AI", "데이터", "리팩토링")
        repeat(200) {
            val member = members[it % members.size]
            val curation = curationService.createCuration(
                titles[it % titles.size],
                contents[it % contents.size],
                links,
                listOf(tags[it % tags.size], tags[(it + 1) % tags.size]),
                member
            )
            createCommentsForCuration(curation, commentService, members)
        }
    }

    private fun createCommentsForCuration(curation: Curation, commentService: CommentService, members: List<Member>) {
        val comments = listOf(
            "정말 유용한 정보네요! 감사합니다.",
            "이 글 덕분에 많은 도움이 되었습니다.",
            "더 많은 예시가 있으면 좋겠어요.",
            "이 내용에 대한 다른 의견을 듣고 싶습니다.",
            "이 글을 읽고 많은 생각이 들었습니다. 잘 읽었습니다.",
            "좋은 자료 공유해주셔서 감사합니다."
        )
        repeat(3) {
            val commenter = members[(it + 1) % members.size]
            commentService.createComment(
                commenter,
                curation.id!!,
                CommentDto(null, null, null, comments[it % comments.size], null, null)
            )
        }
    }

    private fun createPlaylistData(members: List<Member>, playlistService: PlaylistService) {
        val titles = listOf(
            "Java 기초 가이드", "Spring Boot 베스트 프랙티스", "프론트엔드 개발 모음집", "DevOps와 클라우드 인프라",
            "모던 개발 도구", "Kotlin 시작하기", "Microservices 아키텍처", "React 심화 강좌", "Python 데이터 사이언스", "Agile 개발 방법론"
        )
        val descriptions = listOf(
            "자바 기초 문법부터 객체지향 개념까지 이해할 수 있는 자료 모음",
            "효율적인 Spring Boot 애플리케이션 개발을 위한 핵심 가이드",
            "최신 프론트엔드 프레임워크와 도구들을 비교한 모음집",
            "DevOps 및 클라우드 인프라 구축을 위한 핵심 기술 및 도구들",
            "최신 개발 도구와 생산성 향상 팁을 정리한 자료 모음",
            "Kotlin을 활용한 안드로이드 개발 입문 자료 모음",
            "마이크로서비스 아키텍처 설계 및 구현 자료 모음",
            "React 고급 강좌와 프로젝트 예제 모음",
            "Python을 활용한 데이터 사이언스 및 머신러닝 자료 모음",
            "Agile 및 스크럼 개발 방법론에 대한 자료 모음"
        )
        val linksList = listOf(
            listOf("https://www.javatpoint.com/java-tutorial", "https://docs.oracle.com/en/java/"),
            listOf("https://spring.io/guides", "https://www.baeldung.com/spring-boot"),
            listOf("https://react.dev/", "https://vuejs.org/", "https://angular.io/"),
            listOf("https://aws.amazon.com/devops/", "https://cloud.google.com/"),
            listOf("https://www.jetbrains.com/idea/", "https://code.visualstudio.com/"),
            listOf("https://kotlinlang.org/docs/home.html", "https://developer.android.com/kotlin"),
            listOf("https://microservices.io/", "https://12factor.net/"),
            listOf("https://react.dev/docs/getting-started.html", "https://nextjs.org/"),
            listOf("https://www.kaggle.com/", "https://scikit-learn.org/stable/"),
            listOf("https://www.scrum.org/", "https://www.agilealliance.org/")
        )
        titles.indices.forEach { i ->
            val member = members[i % members.size]
            val dto = PlaylistCreateDto(
                titles[i],
                descriptions[i],
                true
            )

            val playlistDto = playlistService.createPlaylist(dto, member)
            val playlist = playlistRepository.findById(playlistDto.id).get()
            val curation = curationRepository.findById(1L).get()
            val item = PlaylistItem(
                itemId = 1L,
                parentItemId = null,
                itemType = PlaylistItem.PlaylistItemType.CURATION,
                playlist = playlist,
                curation = curation,
                displayOrder = 0
            )

            playlistItemRepository.save(item)
        }
    }
}
