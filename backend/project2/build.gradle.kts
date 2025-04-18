plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"

    kotlin("plugin.allopen") version "2.0.21"
    kotlin("plugin.noarg") version "2.0.21"

	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
}

// kotlin jpa : 아래의 어노테이션 클래스에 no-arg 생성자를 생성
noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// kotlin jpa : 아래의 어노테이션 클래스를 open class 로 자동 설정
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

group = "com.team8"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets["main"].kotlin.srcDir("build/generated/source/kapt/main")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 기본 의존성
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Swagger OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Devtools (개발용)
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Database (H2)
    runtimeOnly("com.h2database:h2")

    // Database (MySQL)
    implementation("mysql:mysql-connector-java:8.0.33")

  	// Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
  	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.security:spring-security-test")
  	testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // 외부 링크의 메타 데이터 추출 라이브러리 Jsoup
    implementation("org.jsoup:jsoup:1.15.4")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.30.37")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("io.mockk:mockk:1.13.7") // 최신 버전 사용 권장
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")

    // QueryDSL (JPA & Kotlin용)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

tasks.test {
    useJUnitPlatform()
}
