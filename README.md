# 📸 RealLife (Working Title)
### Real moments. Real people. Real life.

Spring Boot 기반 백엔드 프로젝트로,  
**“리얼한 삶을 공유하는 SNS(인스타그램 스타일)”**를 목표로 합니다.

JWT 인증, 테스트 기반 문서화(REST Docs), 실제 배포까지 고려한  
**실서비스 지향 프로젝트**이며,  
장기적으로는 **무료 서버 배포 → 실제 사용자 이용 → 모바일 앱 스토어 등록**을 목표로 합니다.

---

## 🧭 Project Vision

- 필터링된 삶 ❌
- 과장된 인플루언서 콘텐츠 ❌

있는 그대로의 일상과 감정을 공유하는 SNS를 지향합니다.

### 핵심 컨셉
- 일상 사진 및 글 공유
- 좋아요 · 댓글 · 팔로우
- 알고리즘보다 사람 중심
- 광고 최소화, 사용자 경험 중심

---

## 🛠 Tech Stack

### Backend
- Java 17
- Spring Boot 4.0.2
- Spring Security (Stateless)
- Spring Data JPA (Hibernate)
- JWT (Access Token)
- Spring REST Docs (MockMvc)
- QueryDSL

### Testing & Docs
- JUnit 5
- Spring REST Docs (MockMvc)
- H2 (Test Profile)

### Database
- MySQL

### Frontend
- Vue.js (별도 프로젝트)
- Axios

### Tools
- IntelliJ IDEA (Community)
- Gradle
- Git / GitHub
- Postman

### DevOps (Planned)
- Docker / Docker Compose
- Nginx (Reverse Proxy / Static / Gzip / HTTPS)
- GitHub Actions (CI/CD)
- Free Hosting (Render / Railway / Fly.io / OCI Free Tier)

---

## ✨ Implemented Features

### Auth / User
- 회원가입 API (`POST /api/users`)
- 로그인 API (`POST /api/auth/login`)
  - JWT Access Token 발급
- 보호 API (`GET /api/me`)
  - JWT 인증 필요

### SNS Core
- 게시글 생성/조회/삭제
- 좋아요 / 좋아요 취소
- 팔로우 / 언팔로우
- 팔로우 기반 피드 조회 (**Cursor 기반 페이징**)

### Messaging
- 1:1 대화방 기반 메시지 전송/조회
- 메시지 파일 첨부 (로컬 스토리지)
- 커서 기반 메시지 페이징

### Notification
- 메시지 전송 시 이벤트 발행 → 상대방에게 알림 생성
  - `MessageSentEvent` → `@TransactionalEventListener(AFTER_COMMIT)` 기반
- 내 알림 조회 / 읽음 처리 / 전체 읽음 / 읽은 알림 일괄 삭제(soft delete)
- 중복 알림 방지 로직(존재 여부 체크 기반)

### File
- 파일 업로드 API
- 메시지 첨부 파일 다운로드
- 스토리지 추상화 (Local → S3 교체 가능)

### Error Handling / Docs
- 에러 응답 표준화 (`ErrorResponse`)
- Spring REST Docs 기반 API 문서 자동 생성
- `/docs` 경로로 문서 서빙

---

## 📚 API Documentation

- Local: http://localhost:8080/docs
- GitHub Pages: https://programer5.github.io/vue-spring-backend/

```bash
./gradlew clean test asciidoctor -Dspring.profiles.active=test
```

---

## 📁 Project Structure

```csharp
src
└─ main
   ├─ java
   │  └─ com.example.backend
   │     ├─ config           # Security, JPA, Logging, Querydsl
   │     ├─ controller       # REST API
   │     ├─ domain           # Entity, Aggregate Root
   │     ├─ repository       # JPA + QueryDSL
   │     ├─ service          # Business Logic
   │     ├─ security         # JWT
   │     ├─ exception        # Error Handling
   │     └─ logging          # MDC RequestId
   └─ resources
      └─ static/docs         # REST Docs
```

---

```md
## 🧩 Profiles

```text
- default : MySQL (local/dev)
- test    : H2 in-memory DB + test JWT props (CI & REST Docs)
```

---

## ⚙️ Local Environment Setup

### MySQL Database 생성

```sql
CREATE DATABASE backend
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

---

### 1) Create `application-local.yml` (DO NOT COMMIT)

`src/main/resources/application-local.yml`

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/backend?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: YOUR_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_KEY_32_BYTES_MIN"
  accessTokenExpMinutes: 60
```

---

## 🚀 Run Application

```bash
./gradlew bootRun -Dspring.profiles.active=local
```

---

```md
## 🧪 Test & Documentation

```bash
./gradlew clean test asciidoctor -Dspring.profiles.active=test
```

---

```md
## 🚀 Deployment

- REST Docs is deployed automatically to GitHub Pages on every push to `main`.
```

---

## 🧱 Architecture Strategy (MSA Ready)

```text
본 프로젝트는 초기에는 모놀리식 구조로 시작하되,
도메인 단위 분리 → 마이크로서비스(MSA) 로 점진적 전환이 가능하도록 설계되었습니다.

현재 단계: Modular Monolith
 - 도메인별 패키지 분리
 - 명확한 Service / Repository 경계
 - 도메인 이벤트 확장 가능 구조
  
  향후 MSA 전환 전략
    - Auth / User Service
    - Post / Feed Service
    - Messaging Service
    - File Service
    - Notification Service
  각 서비스는:
    - 독립적인 DB
    - JWT 기반 인증
    - 이벤트(Kafka) 기반 통신
```

---

## ✅ Roadmap

```text
Phase 1 — Core Backend (완료)
- JWT 기반 인증
- 회원가입 / 로그인
- 보호 API (/api/me)
- 에러 응답 표준화
- REST Docs 문서 자동화 + /docs 서빙 + 스타일링

Phase 2 — SNS 기능 (진행/확장)
- 게시글(사진/텍스트) CRUD
- 댓글(Comment) CRUD
- 좋아요 / 팔로우
- 피드 조회 (팔로우 기반 + 최신순 + Cursor)

Phase 2.1 — Messaging (완료)
- DM(1:1) 대화방
- 메시지 전송/조회(커서)
- 파일 업로드/첨부(로컬 → S3 교체 가능)
- 메시지 조회 API REST Docs 문서화

Phase 2.2 — Notification (완료)
- 이벤트 기반 알림 생성(MessageSentEvent 등)
- 알림 조회/읽음/전체읽음/읽은알림 일괄삭제
- 중복 알림 방지 로직(존재 여부 체크 기반)

Phase 2.2+ — Notification (다음)
- 알림 목록 Cursor 기반 페이징 적용
- 중복 알림 방지 고도화(동시성/DB 유니크 방어)

Phase 2.3 — Search (계획 / 적정 타이밍에 진행)
- 사용자 검색 (handle / name 기반)
  - prefix match + 정렬(정확 일치/접두 우선)
- 키워드 검색 (예: "맛집" → 관련 게시글/해시태그/유저)
- (고도화) Elasticsearch/OpenSearch 도입
  - 한국어 검색(분석기), 오타/유사어 대응
  - 통합 검색(유저/게시글/해시태그) + 랭킹 튜닝(인기/최신/관련도)
  - 색인 동기화 전략(이벤트 기반/배치 등)

Phase 4 — DevOps / 운영
- Docker / Docker Compose (MySQL 포함)
- Nginx Reverse Proxy 적용
  - /api → Spring Boot 프록시
  - /docs → 정적 문서 서빙
  - gzip 압축, 캐시, 업로드 제한
  - HTTPS(무료 인증서: Let's Encrypt) + 자동 갱신(서버 환경에서)
- CI/CD (GitHub Actions: test → docs → build)
- 운영 로그 / 모니터링
- 공통 로그(MDC RequestId) + 요청/응답 시간 측정 + 에러 로깅 표준화

Phase 5 — Product Expansion
- 실제 사용자 공개 베타
- Android 앱 출시 (Google Play)
- iOS 앱 출시 (Apple App Store)
- 개인정보 보호 / 약관 정비