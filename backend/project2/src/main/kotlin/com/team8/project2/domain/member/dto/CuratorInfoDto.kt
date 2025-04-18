package com.team8.project2.domain.member.dto

data class CuratorInfoDto(
    val username: String,
    val profileImage: String,
    val introduce: String,
    val curationCount: Long,
    val isFollowed: Boolean,
    val isLogin: Boolean
)