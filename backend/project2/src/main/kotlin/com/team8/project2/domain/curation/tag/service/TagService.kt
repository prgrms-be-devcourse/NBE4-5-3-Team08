package com.team8.project2.domain.curation.tag.service

import com.team8.project2.domain.curation.tag.dto.TagResDto
import com.team8.project2.domain.curation.tag.entity.Tag
import com.team8.project2.domain.curation.tag.repository.TagRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 태그(Tag) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 태그 조회 및 생성 기능을 제공합니다.
 */
@Service
class TagService(
    private val tagRepository: TagRepository,
) {
    /**
     * 태그가 존재하면 기존 태그를 반환하고, 존재하지 않으면 새로 생성하여 반환합니다.
     * @param name 태그 이름
     * @return 기존 또는 새로 생성된 태그 객체
     */
    fun getTag(name: String): Tag = tagRepository.findByName(name) ?: run {
        tagRepository.save(Tag(name))
    }

    // 많이 사용된 tag 수 반환
    @Transactional(readOnly = true)
    fun getTrendingTag(): TagResDto {
        val topTags = tagRepository.findTopTagsByCurationCountDesc(Pageable.ofSize(5))
        return TagResDto.of(topTags)
    }
}
