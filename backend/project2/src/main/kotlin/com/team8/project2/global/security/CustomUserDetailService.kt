package com.team8.project2.global.security

import com.team8.project2.domain.member.repository.MemberRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(
    private val memberRepository: MemberRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberRepository.findByLoginId(username)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")

        return SecurityUser(
            id = member.id!!,
            username = member.getLoginId(),
            password = member.getPassword(),
            authorities = member.authorities,
        )
    }
}
