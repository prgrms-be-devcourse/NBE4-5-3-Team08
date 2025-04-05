package com.team8.project2.global.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.team8.project2.domain.comment.dto.CommentDto;
import com.team8.project2.domain.comment.service.CommentService;
import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.curation.repository.CurationRepository;
import com.team8.project2.domain.curation.curation.service.CurationService;
import com.team8.project2.domain.image.service.S3Uploader;
import com.team8.project2.domain.member.entity.Follow;
import com.team8.project2.domain.member.entity.Member;
import com.team8.project2.domain.member.entity.RoleEnum;
import com.team8.project2.domain.member.repository.FollowRepository;
import com.team8.project2.domain.member.repository.MemberRepository;
import com.team8.project2.domain.playlist.dto.PlaylistCreateDto;
import com.team8.project2.domain.playlist.dto.PlaylistDto;
import com.team8.project2.domain.playlist.entity.Playlist;
import com.team8.project2.domain.playlist.entity.PlaylistItem;
import com.team8.project2.domain.playlist.repository.PlaylistItemRepository;
import com.team8.project2.domain.playlist.repository.PlaylistRepository;
import com.team8.project2.domain.playlist.service.PlaylistService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

	private final MemberRepository memberRepository;
	private final CurationRepository curationRepository;
	private final PlaylistRepository playlistRepository;
	private final CurationService curationService;
	private final S3Uploader s3Uploader;
	private final PlaylistItemRepository playlistItemRepository;

	@Bean
	public ApplicationRunner init(CommentService commentService, FollowRepository followRepository, PlaylistService playlistService) {
		return args -> {
			if (memberRepository.count() == 0 && curationRepository.count() == 0) {
				// 사용자 추가
				List<Member> members = createMembers();

				// 사용자 간 팔로우 관계 설정
				createFollowRelations(members, followRepository);

				// 큐레이션과 댓글 추가
				createCurationData(members, curationService, commentService);

				// 플레이리스트 추가
				createPlaylistData(members, playlistService);
			}
		};
	}

	private List<Member> createMembers() {
		List<Member> members = new ArrayList<>();
		members.add(createMember("team8@gmail.com", "team8", "memberId", "username", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test", RoleEnum.MEMBER));
		members.add(createMember("team9@gmail.com", "team9", "othermember", "other", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test2", RoleEnum.MEMBER));
		members.add(createMember("team10@gmail.com", "team10", "othermember2", "other2", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test3", RoleEnum.MEMBER));
		members.add(createMember("admin@gmail.com", "admin", "admin", "admin", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "admin", RoleEnum.ADMIN));


		members.add(createMember("team11@gmail.com", "team11", "member11", "username11", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test11", RoleEnum.MEMBER));
		members.add(createMember("team12@gmail.com", "team12", "member12", "username12", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test12", RoleEnum.MEMBER));
		members.add(createMember("team13@gmail.com", "team13", "member13", "username13", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test13", RoleEnum.MEMBER));
		members.add(createMember("team14@gmail.com", "team14", "member14", "username14", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test14", RoleEnum.MEMBER));
		members.add(createMember("team15@gmail.com", "team15", "member15", "username15", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test15", RoleEnum.MEMBER));
		members.add(createMember("team16@gmail.com", "team16", "member16", "username16", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test16", RoleEnum.MEMBER));
		members.add(createMember("team17@gmail.com", "team17", "member17", "username17", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test17", RoleEnum.MEMBER));
		members.add(createMember("team18@gmail.com", "team18", "member18", "username18", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test18", RoleEnum.MEMBER));
		members.add(createMember("team19@gmail.com", "team19", "member19", "username19", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test19", RoleEnum.MEMBER));
		members.add(createMember("team20@gmail.com", "team20", "member20", "username20", "password", s3Uploader.getBaseUrl() + "default-profile.svg", "test20", RoleEnum.MEMBER));

		for (int i = 21; i <= 50; i++) {
			members.add(createMember(
				"team" + i + "@gmail.com",
				"team" + i,
				"member" + i,
				"username" + i,
				"password",
				s3Uploader.getBaseUrl() + "default-profile.svg",
				"test" + i,
				RoleEnum.MEMBER
			));
		}

		return members;
	}

	private Member createMember(String email, String username, String memberId, String displayName, String password, String profileImage, String introduce, RoleEnum role) {
		Member member = Member.builder()
				.email(email)
				.role(role)
				.memberId(memberId)
				.username(displayName)
				.password(password)
				.profileImage(profileImage)
				.introduce(introduce)
				.build();
		return memberRepository.save(member);
	}

	private void createFollowRelations(List<Member> members, FollowRepository followRepository) {
		Follow follow1 = new Follow();
		follow1.setFollowerAndFollowee(members.get(2), members.get(0));
		followRepository.save(follow1);

		Follow follow2 = new Follow();
		follow2.setFollowerAndFollowee(members.get(2), members.get(1));
		followRepository.save(follow2);
	}

	private void createCurationData(List<Member> members, CurationService curationService, CommentService commentService) {
		// 큐레이션에 사용할 데이터 준비
		String[] titles = {
				"최신 개발 트렌드", "웹 디자인 팁", "프로그래밍 언어 비교", "AI와 머신러닝의 미래", "클라우드 서비스 비교",
				"효율적인 팀워크 구축법", "DevOps와 CI/CD 도입 방법", "자바스크립트 프레임워크 비교", "Python으로 데이터 분석 시작하기", "리팩토링과 유지보수"
		};

		String[] contents = {
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
		};

		String[] links = {
				"https://www.naver.com/", "https://www.github.com/", "https://www.stackoverflow.com/", "https://www.medium.com/"
		};

		String[] tags = {
				"개발", "프로그래밍", "웹", "디자인", "기술", "클라우드", "팀워크", "AI", "데이터", "리팩토링"
		};

		// 10개의 큐레이션 생성
		for (int i = 0; i < 200; i++) {
			Member member = members.get(i % members.size()); // 사용자 순환

			// 큐레이션 생성
			Curation curation = curationService.createCuration(
					titles[i % titles.length],
					contents[i % contents.length],
					Arrays.asList(links),
					Arrays.asList(tags[i % tags.length], tags[(i + 1) % tags.length]),
					member
			);

			// 댓글 추가 (다양한 사용자들이 댓글을 남김)
			createCommentsForCuration(curation, commentService, members);
		}
	}

	private void createCommentsForCuration(Curation curation, CommentService commentService, List<Member> members) {
		String[] comments = {
				"정말 유용한 정보네요! 감사합니다.",
				"이 글 덕분에 많은 도움이 되었습니다.",
				"더 많은 예시가 있으면 좋겠어요.",
				"이 내용에 대한 다른 의견을 듣고 싶습니다.",
				"이 글을 읽고 많은 생각이 들었습니다. 잘 읽었습니다.",
				"좋은 자료 공유해주셔서 감사합니다."
		};

		// 댓글을 랜덤한 사용자들이 남김
		for (int i = 0; i < 3; i++) {
			Member commenter = members.get((i + 1) % members.size());
			commentService.createComment(
					commenter,
					curation.getId(),
					CommentDto.builder()
							.content(comments[i % comments.length])
							.build()
			);
		}
	}


	private void createPlaylistData(List<Member> members, PlaylistService playlistService) {
		String[] playlistTitles = {
				"Java 기초 가이드",
				"Spring Boot 베스트 프랙티스",
				"프론트엔드 개발 모음집",
				"DevOps와 클라우드 인프라",
				"모던 개발 도구",
				"Kotlin 시작하기",
				"Microservices 아키텍처",
				"React 심화 강좌",
				"Python 데이터 사이언스",
				"Agile 개발 방법론"
		};

		String[] playlistDescriptions = {
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
		};

		List<List<String>> playlistLinks = List.of(
				List.of("https://www.javatpoint.com/java-tutorial", "https://docs.oracle.com/en/java/"),
				List.of("https://spring.io/guides", "https://www.baeldung.com/spring-boot"),
				List.of("https://react.dev/", "https://vuejs.org/", "https://angular.io/"),
				List.of("https://aws.amazon.com/devops/", "https://cloud.google.com/"),
				List.of("https://www.jetbrains.com/idea/", "https://code.visualstudio.com/"),
				List.of("https://kotlinlang.org/docs/home.html", "https://developer.android.com/kotlin"),
				List.of("https://microservices.io/", "https://12factor.net/"),
				List.of("https://react.dev/docs/getting-started.html", "https://nextjs.org/"),
				List.of("https://www.kaggle.com/", "https://scikit-learn.org/stable/"),
				List.of("https://www.scrum.org/", "https://www.agilealliance.org/")
		);

		for (int i = 0; i < playlistTitles.length; i++) {
			Member member = members.get(i % members.size());

			PlaylistCreateDto playlistCreateDto = PlaylistCreateDto.builder()
					.title(playlistTitles[i])
					.description(playlistDescriptions[i])
					.isPublic(true)
					.build();

			PlaylistDto playlistDto = playlistService.createPlaylist(playlistCreateDto, member);
			Playlist playlist = playlistRepository.findById(playlistDto.getId()).get();

			// 플레이리스트에 큐레이션(1L) 추가
			Curation curation = curationRepository.findById(1L).get();
			PlaylistItem newItem = PlaylistItem.builder()
				.itemId(1L)
				.curation(curation)
				.itemType(PlaylistItem.PlaylistItemType.CURATION)
				.playlist(playlist)
				.displayOrder(0)
				.build();
			playlistItemRepository.save(newItem);
		}
	}
}
