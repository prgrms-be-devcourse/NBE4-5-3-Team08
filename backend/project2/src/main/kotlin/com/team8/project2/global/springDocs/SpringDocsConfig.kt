package com.team8.project2.global.springDocs

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    info = Info(
        title = "Project2 API 문서",
        version = "v1",
        description = "팀8의 프로젝트 API 문서입니다."
    )
)
@Configuration
class SpringDocsConfig {
    // API V1 그룹 (명확한 URL prefix 설정)
    @Bean
    fun apiV1Group(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("apiV1")
            .pathsToMatch("/api/v1/**")
            .build()
    }

    // 컨트롤러 그룹 (API 관련 문서 제외)
    @Bean
    fun controllerGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("controller")
            .pathsToExclude("/api/**")
            .build()
    }
}
