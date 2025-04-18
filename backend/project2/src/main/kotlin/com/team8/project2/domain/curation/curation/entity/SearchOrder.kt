package com.team8.project2.domain.curation.curation.entity

/**
 * 큐레이션 검색 정렬 기준을 정의하는 열거형(enum) 클래스입니다.
 */
enum class SearchOrder {
    /** 최신순 정렬  */
    LATEST,

    /** 오래된순 정렬  */
    OLDEST,

    /** 좋아요순 정렬  */
    LIKECOUNT
}
