package com.team8.project2.domain.link.dto


/**
 * 링크 응답 DTO 클래스입니다.
 * 클라이언트에 전달할 링크 데이터를 변환하여 제공합니다.
 */
data class LinkResDTO(
    /**
     * 링크 URL
     * 링크 TITLE
     * 링크 DESCRIPTION
     * 링크 IMAGE
     */
    val url: String,
    val title: String,
    val description: String,
    val image: String,
    val click: Int,
    val linkId: Long
)
