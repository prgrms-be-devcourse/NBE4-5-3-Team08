package com.team8.project2.standard.util

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class TimeZoneConfig {
    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        println("[TimeZoneConfig] JVM 기본 시간대가 KST로 설정되었습니다.")
    }
}