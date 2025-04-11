package com.team8.project2.global

import com.team8.project2.domain.member.entity.Member
import com.team8.project2.domain.member.service.MemberService
import com.team8.project2.global.exception.ServiceException
import com.team8.project2.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class Rq(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    private val memberService: MemberService,
) {
    private val log = LoggerFactory.getLogger(Rq::class.java)

    fun setLogin(member: Member) {
        val authentication = getAuthentication(member)
        SecurityContextHolder.getContext().authentication = authentication
    }

    fun getAuthentication(member: Member): Authentication {
        val userDetails: UserDetails =
            SecurityUser(
                member.id!!,
                member.getMemberId(),
                "",
                member.authorities,
            )
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }

    val actor: Member
        get() {
            val authentication =
                SecurityContextHolder.getContext().authentication
                    ?: throw ServiceException("401-2", "로그인이 필요합니다.")

            if (!authentication.isAuthenticated) {
                throw ServiceException("401-2", "로그인이 필요합니다.")
            } else {
                log.info("Authentication: {}", authentication)
                log.info("Authentication class: {}", authentication.javaClass)
                log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated)
                log.info("Authentication principal: {}", authentication.principal)
            }

            val principal = authentication.principal
            if (principal !is SecurityUser) {
                log.info("[principal class] : {}", principal::class.java)
                log.info("[principal] : {}", principal)
                throw ServiceException("401-3", "잘못된 인증 정보입니다.")
            }

            return memberService
                .findById(principal.id)
        }

    val isLogin: Boolean
        get() {
            val authentication = SecurityContextHolder.getContext().authentication
            return authentication != null &&
                authentication !is AnonymousAuthenticationToken &&
                authentication.isAuthenticated
        }

    fun getHeader(name: String): String? = request.getHeader(name)

    fun getValueFromCookie(name: String): String? {
        val cookies = request.cookies ?: return null
        return cookies.firstOrNull { it.name == name }?.value
    }

    fun setHeader(
        name: String,
        value: String,
    ) {
        response.setHeader(name, value)
    }

    fun addCookie(
        name: String,
        value: String,
    ) {
        val cookie =
            Cookie(name, value).apply {
                domain = "localhost"
                path = "/"
                isHttpOnly = true
                secure = false
                setAttribute("SameSite", "Strict")
            }
        response.addCookie(cookie)
    }

    fun getRealActor(actor: Member): Member =
        memberService
            .findById(actor.id)

    fun removeCookie(name: String) {
        val cookie =
            Cookie(name, null).apply {
                domain = "localhost"
                path = "/"
                isHttpOnly = true
                secure = true
                setAttribute("SameSite", "Strict")
                maxAge = 0
            }
        response.addCookie(cookie)
    }
}
