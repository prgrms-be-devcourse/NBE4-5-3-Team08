package com.team8.project2.domain.curation.like.repository

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.curation.like.entity.Like
import com.team8.project2.domain.curation.like.entity.Like.LikeId
import com.team8.project2.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 큐레이션 좋아요(Like) 엔티티의 데이터 접근을 담당하는 리포지토리 인터페이스입니다.
 */
interface LikeRepository : JpaRepository<Like, LikeId> {
    /**
     * 특정 큐레이션과 특정 회원에 대한 좋아요 정보를 조회합니다.
     * @param curation 좋아요를 조회할 큐레이션
     * @param member 좋아요를 조회할 회원
     * @return 좋아요 정보
     */
    fun findByCurationAndMember(curation: Curation, member: Member): Like?
}
