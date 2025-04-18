package com.team8.project2.domain.curation.tag.repository

import com.team8.project2.domain.curation.tag.entity.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 태그(Tag) 데이터를 관리하는 레포지토리 인터페이스입니다.
 * 태그 이름을 기반으로 검색하는 기능을 포함하고 있습니다.
 */
@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    /**
     * 태그 이름을 기반으로 태그를 조회합니다.
     * @param name 태그 이름
     * @return 해당 이름을 가진 태그 객체 (없을 경우 빈 Optional 반환)
     */
    fun findByName(name: String?): Tag?

    /**
     * 태그와 연관된 큐레이션 개수를 기준으로 내림차순 정렬하여 상위 5개만 가져오는 메서드
     */
    @Query("SELECT t FROM Tag t LEFT JOIN t.curationTags ct GROUP BY t ORDER BY COUNT(ct) DESC")
    fun findTopTagsByCurationCountDesc(pageable: Pageable): List<Tag>
}
