package com.team8.project2.domain.member.service

import com.team8.project2.domain.member.entity.Member
import com.team8.project2.standard.util.Ut
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import lombok.RequiredArgsConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@RequiredArgsConstructor
class AuthTokenService {
    @Value("\${custom.jwt.secret-key}")
    private val keyString: String? = null

    @Value("\${custom.jwt.expire-seconds}")
    private val expireSeconds = 0

    @Transactional
    fun genAccessToken(member: Member): String {
        val secretKey = Keys.hmacShaKeyFor(keyString!!.toByteArray())
        val a = Ut.Jwt.createToken(
            keyString,
            expireSeconds,
            java.util.Map.of<String, Any>(
                "id", member.id,
                "loginId", member.getLoginId()
            )
        )
        val claimsJws = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseClaimsJws(a)

        val expiration = claimsJws.body.expiration
        log.info("[body expiration]$expiration")

        return a
    }

    fun getPayload(token: String?): Map<String, Any?>? {
        if (!Ut.Jwt.isValidToken(keyString.toString(), token!!)) return null

        val payload = Ut.Jwt.getPayload(keyString.toString(), token)
        val idNo = payload["id"] as Number?
        val id = idNo!!.toLong()
        val loginId = payload["loginId"] as String?
        return java.util.Map.of<String, Any?>(
            "id", id,
            "loginId", loginId
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AuthTokenService::class.java)
    }
}
