package com.team8.project2.domain.curation.tag.dto

/**
 * 태그 생성 요청을 위한 DTO 클래스입니다.
 * 클라이언트가 태그 데이터를 전송할 때 사용됩니다.
 */
data class TagReqDto(
    /**
     * 태그 이름
     */
    val name: String,
)
