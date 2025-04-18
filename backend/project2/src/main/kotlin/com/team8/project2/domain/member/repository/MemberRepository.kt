package com.team8.project2.domain.member.repository

import com.team8.project2.domain.member.entity.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository


interface MemberRepository : JpaRepository<Member, Long> {
    fun findByLoginId(loginId: String?): Member?
    fun findByUsername(username: String?): Member?
    override fun findAll(pageable: Pageable): Page<Member>
}
