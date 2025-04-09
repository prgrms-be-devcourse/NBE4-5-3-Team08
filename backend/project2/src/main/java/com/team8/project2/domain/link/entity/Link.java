package com.team8.project2.domain.link.entity;

import com.team8.project2.domain.curation.curation.entity.CurationLink;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 링크(Link) 엔티티 클래스입니다.
 * 큐레이션과 연관된 외부 링크 정보를 저장합니다.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Link")
public class Link {

	/**
	 * 링크의 고유 ID (자동 생성)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "linkId", nullable = false)
	public Long id; // kotlin 전환 임시 public

	/**
	 * 링크 URL (필수값)
	 */
	@Column(name = "url", nullable = false)
	private String url;

	/**
	 * 링크 클릭 수 (기본값 0)
	 */
	@Column(name = "click", nullable = false)
	private int click = 0;

	/**
	 * 링크 생성 시간 (자동 설정)
	 */
	@CreatedDate
	@Setter(AccessLevel.PRIVATE)
	@Column(name = "createdAt", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 링크와 연관된 큐레이션 목록 (1:N 관계)
	 */
	@OneToMany(mappedBy = "link")
	private List<CurationLink> curationLinks;

	/**
	 * 링크 제목
	 */
	@Column(name = "title")
	private String title;

	/**
	 * 링크 설명
	 */
	@Column(name = "description")
	private String description;

	@Column(name = "metaImageUrl")
	private String metaImageUrl;

	public void loadMetadata() {
		try {
			// Jsoup을 사용하여 URL의 HTML을 파싱
			Document doc = Jsoup.connect(url).get();
			this.title = getMetaTagContent(doc, "og:title");
			this.description = getMetaTagContent(doc, "og:description");
			this.metaImageUrl = getMetaTagContent(doc, "og:image");
		} catch (IOException e) {
			this.title = this.url;
			this.description = this.url;
		}
	}

	private String getMetaTagContent(Document doc, String property) {
	    Element metaTag = doc.select("meta[property=" + property + "]").first();
	    return metaTag != null ? metaTag.attr("content") : "";
	}
}