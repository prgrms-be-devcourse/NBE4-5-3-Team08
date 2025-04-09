package com.team8.project2.domain.curation.curation.dto;

import java.util.List;

import com.team8.project2.domain.curation.curation.entity.Curation;
import lombok.Getter;

@Getter
public class TrendingCurationResDto {

	List<CurationSummaryResDto> curations;

	public static TrendingCurationResDto of(List<Curation> curations) {
		TrendingCurationResDto dto = new TrendingCurationResDto();
		dto.curations = curations.stream()
			.map(curation -> CurationSummaryResDto.of(curation))
			.toList();
		return dto;
	}
}
