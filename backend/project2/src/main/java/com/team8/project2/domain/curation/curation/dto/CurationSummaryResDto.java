package com.team8.project2.domain.curation.curation.dto;

import com.team8.project2.domain.curation.curation.entity.Curation;
import lombok.Getter;

@Getter
public class CurationSummaryResDto {
	private Long curationId;
	private String title;
	private String authorName;
	private Long viewCount;

	public static CurationSummaryResDto of(Curation curation) {
		CurationSummaryResDto dto = new CurationSummaryResDto();
		dto.curationId = curation.getId();
		dto.title = curation.getTitle();
		dto.authorName = curation.getMemberName();
		dto.viewCount = curation.getViewCount();
		return dto;
	}
}
