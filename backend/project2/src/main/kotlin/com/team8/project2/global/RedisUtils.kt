package com.team8.project2.global

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisUtils(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    // 새로 만든 prefix 기반 삭제
    fun clearKeysByPattern(pattern: String = "*") {
        val keys = redisTemplate.keys(pattern)
        if (!keys.isNullOrEmpty()) {
            redisTemplate.delete(keys)
            println("Deleted Redis keys matching pattern: $pattern")
        } else {
            println("No Redis keys found matching pattern: $pattern")
        }
    }

    // 이전 방식 호환용: 전체 삭제 메서드 유지
    fun clearAllData() {
        clearKeysByPattern("*")
    }

}
