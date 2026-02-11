# 📸 RealLife (Working Title)
### Real moments. Real people. Real life.

Spring Boot 기반 백엔드 프로젝트로,  
**“리얼한 삶을 공유하는 SNS(인스타그램 스타일)”**를 목표로 합니다.

JWT 인증, 테스트 기반 문서화(REST Docs), Flyway 기반 DB 마이그레이션,  
실제 배포까지 고려한 **실서비스 지향 프로젝트**입니다.

장기적으로는  
무료 서버 배포 → 실제 사용자 이용 → 모바일 앱 스토어 등록을 목표로 합니다.

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
- QueryDSL (Cursor Paging / 검색 최적화)
- JWT (Access Token)
- **Flyway (DB Migration)**

### Testing & Docs
- JUnit 5
- Spring REST Docs (MockMvc)
- H2 (Test Profile)

### Database
- MySQL

### Frontend
- Vue.js (별도 프로젝트)
- Axios

### DevOps (Planned)
- Docker / Docker Compose
- Nginx (Reverse Proxy / HTTPS / Gzip)
- GitHub Actions (CI/CD)
- Free Hosting (Render / Railway / Fly.io / OCI Free Tier)

---

## ✨ Implemented Features

### Auth / User
- 회원가입 API (`POST /api/users`)
- 로그인 API (`POST /api/auth/login`)
- JWT Access Token 발급
- 보호 API (`GET /api/me`)

### SNS Core
- 게시글 생성 / 조회 / 삭제
- 댓글 생성 / 목록 조회 / 삭제
- 좋아요 / 좋아요 취소
- 팔로우 / 언팔로우
- 팔로우 기반 피드 조회 (**Cursor 기반 페이징**)

### Messaging
- 1:1 대화방 메시지 전송/조회
- 파일 첨부 (Local → S3 확장 가능)
- Cursor 기반 메시지 페이징

### Notification
- 이벤트 기반 알림 생성
  - `@TransactionalEventListener(AFTER_COMMIT)`
- 읽음 처리 / 전체 읽음 / 읽은 알림 soft delete
- 중복 알림 방지 로직

### DB Migration
- Flyway 기반 스키마 관리
- baseline 전략 적용
- 인덱스 변경도 migration으로 관리
- `flyway_schema_history` 테이블로 버전 추적

---

## 📚 API Documentation

- Local: http://localhost:8080/docs
- GitHub Pages: https://programer5.github.io/vue-spring-backend/

```bash
./gradlew clean test asciidoctor copyRestDocs -Dspring.profiles.active=test
```

---

## 📁 Project Structure

```csharp
src
└─ main
   ├─ java
   │  └─ com.example.backend
   │     ├─ config
   │     ├─ controller
   │     ├─ domain
   │     ├─ repository
   │     ├─ service
   │     ├─ security
   │     ├─ exception
   │     └─ logging
   └─ resources
      ├─ db
      │  └─ migration        # Flyway scripts
      └─ static/docs
```

---

## 🧩 Profiles

```lua
application.yml        → 공통 설정
application-local.yml  → 개인 로컬 환경 (Git 제외)
application-dev.yml    → 개발 환경
application-prod.yml   → 운영 환경
application-test.yml   → 테스트 환경
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

## 🧪 Test & Documentation

```bash
./gradlew clean test asciidoctor -Dspring.profiles.active=test
```

---

## 🗄 DB Migration Strategy (Flyway)

```text
Migration 위치: src/main/resources/db/migration
네이밍 규칙:
  V1__init_schema.sql
  V2__add_comments_cursor_index.sql
  V3__drop_comment_duplicate_index.sql
baseline-on-migrate 전략 사용
운영/로컬 모두 동일한 migration 스크립트 사용
DB 변경은 절대 수동 수정하지 않고 migration으로 관리
```

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## 🧠 Design Decisions

```markdown
UUID 기반 PK
- 순차 ID 노출 방지
- URL 추측 공격 방어

Cursor 기반 페이징
- createdAt + id 조합
- Base64URL Opaque Cursor

연관관계 최소화
- Comment → Post 직접 연관 제거
- postId(UUID)만 보유
```

---

## ⚡ Performance & DB Strategy

```scss
Cursor Pagination 적용
MySQL 인덱스 최적화
EXPLAIN 기반 실행 계획 검증
중복 인덱스 제거(runbook 문서화)
Flyway로 인덱스 변경 관리
```

---

## 🔐 Security Notes

```nginx
Stateless JWT 인증
Controller / Service 계층에서 명시적 권한 검증
공통 ErrorResponse 표준화
환경 변수 기반 시크릿 관리
```

---

## 🚀 Deployment

```diff
main 브랜치 push 시:
- 테스트 실행
- REST Docs 생성
- GitHub Pages 자동 배포

운영 환경에서는:
  Flyway 자동 migrate
  환경 변수 기반 DB / JWT 설정
```

---

## 🧱 Architecture Strategy (MSA Ready)

```text
현재: Modular Monolith

확장 시 분리 가능:
  Auth Service
  Post / Feed Service
  Messaging Service
  Notification Service
  File Service
이벤트 기반(Kafka 등) 통신 구조로 확장 가능
```

---

## 🧠 Key Design Highlights

```text
- Cursor Pagination 기반 대용량 피드/댓글/메시지 조회
- UUID PK + Opaque Cursor로 리소스 추측 방지
- Comment–Post 연관관계 제거로 도메인 결합도 최소화
- EXPLAIN 기반 인덱스 검증 및 중복 인덱스 제거
- 테스트 기반 API 문서 자동화 (REST Docs)
- 설계 결정 문서(ADR): `src/docs/architecture/ARCHITECTURE_DECISIONS.md`
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
```
