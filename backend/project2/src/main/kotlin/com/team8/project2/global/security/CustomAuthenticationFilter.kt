package com.team8.project2.global.security

import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.Rq
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val rq: Rq,
    private val memberService: MemberService
) : OncePerRequestFilter() {

    private fun isAuthorizationHeaderPresent(): Boolean {
        val authorizationHeader = rq.getHeader("Authorization")
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
    }

    private fun extractAccessToken(): String? {
        return if (isAuthorizationHeaderPresent()) {
            rq.getHeader("Authorization")?.removePrefix("Bearer ")?.trim()
        } else {
            rq.getValueFromCookie("accessToken")
        }
    }

    private fun authenticateMember(accessToken: String?): Member? {
        return accessToken?.let { memberService.getMemberByAccessToken(it).orElse(null) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestURI = request.requestURI

        val skipPaths = listOf(
            "/api/v1/members/login",
            "/api/v1/members/join",
            "/api/v1/members/logout"
        )

        if (requestURI in skipPaths) {
            filterChain.doFilter(request, response)
            return
        }

        val accessToken = extractAccessToken()
        val authenticatedMember = authenticateMember(accessToken)

        if (authenticatedMember != null) {
            rq.setLogin(authenticatedMember)
            SecurityContextHolder.getContext().authentication = rq.getAuthentication(authenticatedMember)
        }

        filterChain.doFilter(request, response)
    }
}
