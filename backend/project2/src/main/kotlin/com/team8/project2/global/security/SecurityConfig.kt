package com.team8.project2.global.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests {
                it
                    // Swagger
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                    // Public endpoints
                    .requestMatchers(HttpMethod.GET, "/api/v1/playlists", "/api/v1/playlists/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/members/**", "/api/v1/members/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/members/members").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/members/**", "/api/v1/members/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/curation/**").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/curation/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/curation/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/curation/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/curations/**").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/curations/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/curations/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/curations/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/images/upload").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/link/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/link/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "api/v1/admin/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/h2-console/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/h2-console/**").permitAll()

                    // Admin only
                    .requestMatchers("/api/v1/posts/statistics").hasRole("ADMIN")

                    // All others
                    .anyRequest().authenticated()
            }
            .headers {
                it.addHeaderWriter(
                    XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
                )
            }
            .csrf { it.disable() }
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowCredentials = true
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
