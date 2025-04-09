package com.team8.project2.domain.curation.curation.event

data class CurationUpdateEvent(
    val curationId: Long,
    val imageUrls: List<String>,
)