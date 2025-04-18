package com.team8.project2.standard.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import java.util.*

object Ut {
    private val log = LoggerFactory.getLogger(Ut::class.java)

    object Json {
        private val objectMapper = ObjectMapper()

        @JvmStatic
        fun toString(obj: Any): String {
            try {
                return objectMapper.writeValueAsString(obj)
            } catch (e: JsonProcessingException) {
                throw RuntimeException(e)
            }
        }
    }

    object Jwt {
        @JvmStatic
        fun createToken(keyString: String, expireSeconds: Int, claims: Map<String, Any>): String {
            val secretKey = Keys.hmacShaKeyFor(keyString.toByteArray())

            val issuedAt = Date()
            val expiration = Date(issuedAt.time + 1000L * expireSeconds)
            println("[JVM 시간대] ${TimeZone.getDefault().id}")
            log.info("[expiration] $expiration")
            log.info("[issuedAt] $issuedAt")

            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        @JvmStatic
        fun isValidToken(keyString: String, token: String): Boolean {
            val currentTime = System.currentTimeMillis()
            println("[현재 서버 시간] ${Date(currentTime)}")
            try {
                val secretKey = Keys.hmacShaKeyFor(keyString.toByteArray())

                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(token)

                return true
            } catch (e: ExpiredJwtException) {
                println("[JWT] 토큰 만료됨: ${e.message}")
            } catch (e: SignatureException) {
                println("[JWT] 서명 불일치: ${e.message}")
            } catch (e: MalformedJwtException) {
                println("[JWT] 형식 오류: ${e.message}")
            } catch (e: Exception) {
                println("[JWT] 기타 오류: ${e.message}")
            }
            return false
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun getPayload(keyString: String, jwtStr: String): Map<String, Any> {
            val secretKey = Keys.hmacShaKeyFor(keyString.toByteArray())

            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parse(jwtStr)
                .payload as Map<String, Any>
        }
    }
}