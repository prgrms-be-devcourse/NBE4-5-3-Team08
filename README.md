# NBE4-5-3-Team08
프로그래머스 백엔드 데브코스 4기 5회차 8팀 괜찮아 딩딩딩 3차 팀 프로젝트입니다.

# 🎯 3차 프로젝트 기획서 - Kotlin 전환 및 기능 고도화

## 📌 프로젝트 개요

- **프로젝트명**: 링큐레이션 (LINKURATION)
- **목표**: 기존 Java 기반 프로젝트를 Kotlin으로 전환하고, 기능 보완 및 코드 리팩토링을 통해 유지보수성과 성능 향상

---

## ✅ 핵심 작업 항목

### 1. Kotlin 전환

- Java → Kotlin 전체 코드 마이그레이션
- Entity, DTO, Controller, Service, Repository, Test 등 모든 레이어 포함
- Kotlin 스타일 적용 (data class, nullable, 확장 함수 등)

### 2. 테스트 코드 최신화

- 누락 테스트 코드 보완
- 기존 테스트 코드 Kotlin 기반으로 재작성
- `BaseInitData` 활용한 통합 테스트 유지
- `Mockk`, `SpringBootTest` 등 Kotlin 친화 환경 구성

### 3. 불완전한 기능 보완

- 조회수 저장 방식 개선 (Redis 캐싱 + DB 동기화)
- 좋아요, 신고 등 미완성 기능 보완
- JWT 인증 시 DB 접근 제거 (payload 활용)

### 4. 코드 리팩토링

- 출력문 제거 → 로그로 대체
- Controller → DTO 반환, Entity 직접 노출 금지
- Service → 타 도메인 Repository 접근 권장 (Service 간 호출 지양)
- 불필요한 상속 구조 제거 및 정리
- `@Transactional` 위치 통일

---

## 🛠 기술 개선 계획

### ✅ 인증/인가 최적화

- JWT에 유저 정보 포함 (`id`, `role`, `nickname` 등)
- DB 조회 없이 `SecurityContextHolder`로부터 사용자 구성
- JWT → 가짜 Member 객체 생성 방식 도입

### ✅ CI/CD 및 무중단 배포

- GitHub Actions 기반 자동화 배포
- 무중단 배포 전략 고려 (Rolling Update or Blue-Green)
- Docker + SpringBoot + Nginx + EC2 환경 기반

### ✅ 성능 개선

- k6, InfluxDB, Grafana 통한 부하 테스트
- 병목 지점 식별 및 쿼리 튜닝
- N+1 문제 해결 (`@EntityGraph`, Fetch Join, DTO Projection 등)
- Redis 캐싱 도입 (조회수, 좋아요 등)

### ✅ 코드 컨벤션 통일

- Controller, Service, DTO 작성 규칙 정리
- `@Transactional`, nullable, lateinit 등 일관된 코드 작성 방식 정립

### ✅ 모니터링 (선택사항)

- Spring Actuator + Grafana 기반 APM 도입 검토
- 지표: 가용성, 지연 시간, 오류율, 트래픽 등

---

## 🔍 진행 일정 예시

| 주차  | 주요 내용                             |
|-------|----------------------------------------|
| 1주차 | Kotlin 마이그레이션 시작 (Entity, DTO) |
| 2주차 | Controller/Service 전환, 테스트 작성   |
| 3주차 | 기능 보완 (조회수, JWT 인증 등)        |
| 4주차 | 성능 테스트 및 최적화                  |
| 5주차 | CI/CD 구축, 무중단 배포 적용           |
| 6주차 | 마무리 리팩토링, 컨벤션 정리           |

---

## 📎 예시 코드 - 인증 최적화

```kotlin
fun getActorFromJwt(): Member {
    val authentication = SecurityContextHolder.getContext().authentication
        ?: throw ServiceException("401", "로그인이 필요합니다.")

    val principal = authentication.principal
    if (principal !is SecurityUser) {
        throw ServiceException("401", "잘못된 인증 정보입니다.")
    }

    return Member(
        id = principal.id,
        nickname = principal.nickname,
        // 실제 DB 조회 없이 JWT 정보 기반 구성
    )
}
```

---

## 📄 산출물 리스트
- Kotlin 전환된 전체 코드

- 최신화된 통합/단위 테스트

- Redis 캐싱 기능

- 부하 테스트 리포트 (Grafana 대시보드)

- CI/CD 자동화 파이프라인

