package com.team8.project2.domain.curation.curation.dto;

import java.util.List;

import com.team8.project2.domain.curation.curation.entity.Curation;
import lombok.Getter;

@Getter
public class CurationSearchResDto {

	private List<CurationResDto> curations;
	private int totalPages;
	private long totalElements;
	private int numberOfElements;
	private int size;

	public static CurationSearchResDto of(List<Curation> curations, int totalPages, long totalElements, int numberOfElements, int size) {
		CurationSearchResDto dto = new CurationSearchResDto();
		dto.curations = curations.stream()
			.map(CurationResDto::new)
			.toList();
		dto.totalPages = totalPages;
		dto.totalElements = totalElements;
		dto.numberOfElements = numberOfElements;
		dto.size = size;
		return dto;
	}
}
