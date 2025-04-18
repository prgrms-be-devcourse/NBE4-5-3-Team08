package com.team8.project2.global.init

import org.apache.commons.lang3.SystemUtils
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Profile("dev")
@Configuration
class DevInitData {

    @Bean
    fun devApplicationRunner(): ApplicationRunner = ApplicationRunner {
        val apiDocsUrl = "http://localhost:8080/v3/api-docs/apiV1"
        val apiJsonPath = "../apiV1.json"
        val frontendSchemaPath = "../../frontend/project2/libs/backend/apiV1/schema.d.ts"

        // JSON 생성
        genApiJsonFile(apiDocsUrl, apiJsonPath)

        // 명령어로 타입스크립트 스키마 생성
        runCmd(getGenerateSchemaCommand(apiJsonPath, frontendSchemaPath))
    }

    private fun getGenerateSchemaCommand(jsonPath: String, tsOutputPath: String): List<String> {
        val command = """npx --package typescript --package openapi-typescript --package punycode openapi-typescript $jsonPath -o $tsOutputPath"""
        return if (SystemUtils.IS_OS_WINDOWS) {
            listOf("cmd.exe", "/c", command)
        } else {
            listOf("sh", "-c", command)
        }
    }

    private fun runCmd(command: List<String>) {
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println(line)
                }
            }

            val exitCode = process.waitFor()
            println("명령어 실행 완료, 종료 코드: $exitCode")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun genApiJsonFile(url: String, filename: String) {
        val filePath = Path.of(filename)
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                Files.writeString(filePath, response.body(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                println("✅ JSON 데이터가 저장되었습니다: ${filePath.toAbsolutePath()}")
            } else {
                println("❌ API 문서 다운로드 실패 (상태 코드: ${response.statusCode()})")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
