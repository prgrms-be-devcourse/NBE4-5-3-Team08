package com.team8.project2.domain.link.repository

import com.team8.project2.domain.link.entity.Link
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

/**
 * 링크(Link) 데이터를 관리하는 레포지토리 인터페이스입니다.
 * 링크 URL을 기반으로 검색하는 기능을 포함하고 있습니다.
 */
interface LinkRepository : JpaRepository<Link, Long> {
    /**
     * 링크 URL을 기반으로 링크를 조회합니다.
     * @param url 조회할 링크 URL
     * @return 해당 URL을 가진 링크 객체 (없을 경우 빈 Optional 반환)
     */
    fun findByUrl(url: String): Link?
}
