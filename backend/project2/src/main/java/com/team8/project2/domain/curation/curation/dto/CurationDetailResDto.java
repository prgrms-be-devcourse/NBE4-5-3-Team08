package com.team8.project2.domain.curation.curation.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.team8.project2.domain.comment.dto.ReplyCommentDto;
import com.team8.project2.domain.comment.entity.Comment;
import com.team8.project2.domain.curation.curation.entity.Curation;
import com.team8.project2.domain.curation.tag.entity.Tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurationDetailResDto {
	private Long id;

	/** 큐레이션 제목 */
	private String title;

	/** 큐레이션 내용 */
	private String content;

	/** 큐레이션 작성자 */
	private long authorId;

	private String authorName;

	private String authorImgUrl;

	/** 큐레이션에 포함된 링크 목록 */
	private List<LinkResDto> urls;

	/** 큐레이션에 포함된 태그 목록 */
	private List<TagResDto> tags;

	/** 큐레이션에 포함된 댓글 목록 */
	private List<CommentResDto> comments;

	/** 큐레이션 작성 시각 */
	private LocalDateTime createdAt;

	/** 마지막 큐레이션 변경 시각 */
	private LocalDateTime modifiedAt;

	/** 좋아요 수 */
	private Long likeCount;

	/** 조회수 */
	private Long viewCount;

	/** 좋아요 여부*/
	private boolean isLiked;

	/** 작성자 팔로우 여부*/
	private boolean isFollowed;

	/** 로그인 여부*/
	private boolean isLogin;

	/**
	 * 링크 정보를 포함하는 내부 DTO 클래스
	 */
	@Getter
	static class LinkResDto {
		private Long id;
		private String url;
		private String title;
		private String description;
		private String imageUrl;
		private int click;

		public LinkResDto(Link link) {
			this.id = link.getId();
			this.url = link.getUrl();
			this.title = link.getTitle();
			this.description = link.getDescription();
			this.imageUrl = link.getMetaImageUrl();
			this.click = link.getClick();
		}
	}

	/**
	 * 태그 정보를 포함하는 내부 DTO 클래스
	 */
	@Getter
	static class TagResDto {
		private String name;

		public TagResDto(Tag tag) {
			this.name = tag.getName();
		}
	}

	@Getter
	static class CommentResDto {
		private long commentId;
		private long authorId;
		private String authorName;
		private String authorImgUrl;
		private String content;
		private LocalDateTime createdAt;
		private LocalDateTime modifiedAt;
		private List<ReplyCommentDto> replies;

		public CommentResDto(Comment comment) {
			this.commentId = comment.getId();
			this.authorId = comment.getAuthor().getId();
			this.authorName = comment.getAuthor().getUsername();
			this.authorImgUrl = comment.getAuthor().getProfileImage();
			this.content = comment.getContent();
			this.createdAt = comment.getCreatedAt();
			this.modifiedAt = comment.getModifiedAt();
			this.replies = comment.getReplyComments().stream()
					.map(reply -> ReplyCommentDto.Companion.fromEntity(reply))
					.sorted(Comparator.comparing(ReplyCommentDto::getCreatedAt))
				.toList();
		}
	}

	/**
	 * 엔티티(Curation) 객체를 DTO(CurationDetailResDto)로 변환합니다.
	 *
	 * @param curation   변환할 큐레이션 엔티티
	 * @param isFollowed
	 * @param isLogin
	 * @return 변환된 상세 큐레이션 DTO
	 */
	public static CurationDetailResDto fromEntity(Curation curation, boolean isLiked, boolean isFollowed,
												  boolean isLogin) {
		return CurationDetailResDto.builder()
			.id(curation.getId())
			.title(curation.getTitle())
			.content(curation.getContent())
			.authorId(curation.getMemberId())
			.authorName(curation.getMemberName())
			.authorImgUrl(curation.getMemberImgUrl())
			.urls(curation.getCurationLinks().stream()
				.map(cl -> new LinkResDto(cl.getLink()))
				.collect(Collectors.toList()))
			.tags(curation.getTags().stream()
				.map(tag -> new TagResDto(tag.getTag()))
				.collect(Collectors.toList()))
			.comments(curation.getComments().stream()
				.map(comment -> new CommentResDto(comment))
				.sorted(Comparator.comparing(CommentResDto::getCreatedAt).reversed())
				.collect(Collectors.toList()))
			.createdAt(curation.getCreatedAt())
			.modifiedAt(curation.getModifiedAt())
			.likeCount(curation.getLikeCount())
			.viewCount(curation.getViewCount())
			.isLiked(isLiked)
			.isFollowed(isFollowed)
			.isLogin(isLogin)
			.build();
	}
}
