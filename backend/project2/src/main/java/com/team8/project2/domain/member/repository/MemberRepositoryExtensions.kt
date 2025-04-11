package com.team8.project2.domain.member.repository

import com.team8.project2.domain.member.entity.Member
import com.team8.project2.global.exception.ServiceException

fun MemberRepository.findByIdOrThrow(id: Long?): Member {
    return id?.let {
        this.findByIdOrNull(it)
            ?: throw ServiceException("404-1", "해당 회원을 찾을 수 없습니다.")
    } ?: throw ServiceException("400-1", "ID는 null일 수 없습니다.")
}

fun MemberRepository.findByIdOrNull(id: Long?): Member? {
    return id?.let { this.findById(it).orElse(null) }
}