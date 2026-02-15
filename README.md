# 📸 RealLife (Working Title)
### Real moments. Real people. Real life.

Spring Boot 기반 백엔드 프로젝트로,  
**“리얼한 삶을 공유하는 SNS(인스타그램 스타일)”**를 목표로 합니다.

JWT 인증, 테스트 기반 문서화(REST Docs), Flyway 기반 DB 마이그레이션,  
실서비스 운영을 고려한 **validate 전략 + 이벤트 기반(알림/DM) + 실시간(SSE)** 구조로 개발 중입니다.

---

## 🧭 Project Vision

- 필터링된 삶 ❌
- 과장된 인플루언서 콘텐츠 ❌

있는 그대로의 일상과 감정을 공유하는 SNS를 지향합니다.

### 핵심 컨셉
- 일상 사진 및 글 공유
- 좋아요 · 댓글 · 팔로우
- 사람 중심 피드/관계
- 광고 최소화, 사용자 경험 중심

---

## 🛠 Tech Stack

### Backend
- Java 17
- Spring Boot 4.0.2
- Spring Web / Validation
- Spring Security (JWT)
- Spring Data JPA (Hibernate)
- QueryDSL (Cursor Paging / 검색 최적화)
- Flyway (DB Migration, `ddl-auto=validate`)
- **SSE(Server-Sent Events)**
- **Redis**
  - **Pub/Sub 기반 SSE Fan-out (멀티 인스턴스 대비)**
  - (옵션) Last-Event-ID 재전송을 위한 이벤트 스토어 설계(인터페이스 존재)

### Testing & Docs
- JUnit 5
- Spring REST Docs (MockMvc)
- Spring Security Test
- H2 (test profile)
- Testcontainers (MySQL)

### Database / Cache
- MySQL
- Redis

---

## ✨ Implemented Features (현재 구현)

### Auth / User
- 회원가입 API (`POST /api/users`)
- 로그인 API (`POST /api/auth/login`) → Bearer 토큰 반환
- **브라우저/SSE용 로그인 API (`POST /api/auth/login-cookie`) → HttpOnly 쿠키**
- 보호 API (`GET /api/me`)

### SNS Core
- 게시글 생성 / 조회 / 삭제
- 댓글 생성 / 목록 조회 / 삭제
- 좋아요 / 좋아요 취소
- 팔로우 / 언팔로우
- 팔로우 기반 피드 조회 (**Cursor 기반 페이징**)

### Messaging (DM)
- 1:1 대화방(Direct) + 중복 방지 키
- Conversation 목록: **cursor paging + unreadCount**
- Message 목록: **cursor paging + 조회 시 자동 last_read_at 갱신**
- last message denorm(`last_message_*`) 기반 목록 최적화

### Notification
- 이벤트 기반 알림 생성 (`@TransactionalEventListener(AFTER_COMMIT)`)
- 알림 목록 **cursor paging**
- 읽음 처리 / 전체 읽음 / 읽은 알림 삭제

### Real-time (SSE + Redis Pub/Sub)
- 구독: `GET /api/sse/subscribe`
- 이벤트:
  - `connected` (연결 확인)
  - `ping` (heartbeat)
  - `message-created`
  - `message-deleted`
  - `notification-created`
- Redis Pub/Sub:
  - publish: `sse:push`
  - subscriber가 메시지를 받아 **실제 emitter로 전송**
- (코드 구조) `test` 프로필에서는 Redis 없이 로컬 전송으로 테스트 안정화

> SSE 연결/메시지 전송이 정상 동작하는 로그 예시가 이전 대화 파일에 포함되어 있습니다. :contentReference[oaicite:1]{index=1}

### DB Migration (Flyway)
- Flyway 기반 스키마/인덱스 관리
- `ddl-auto=validate` 전략
- `flyway_schema_history` 기반 버전 추적
- dev에서 `baseline-on-migrate: true` 사용

---

## 📚 API Documentation (REST Docs)

- Local: `http://localhost:8080/docs` (기본 포트, 환경에 따라 변경 가능)
- GitHub Pages(운영 설정 시): 프로젝트 설정에 따라 배포

문서 생성/복사:

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
   │     ├─ logging
   │     └─ sse
   └─ resources
      ├─ db
      │  └─ migration        # Flyway scripts
      └─ static/docs         # REST Docs output
```

---

## 🧩 Profiles

```lua
application.yml        → 공통 설정
application-dev.yml    → 개발 환경 (Flyway ON, ddl-auto=validate, Redis 사용, SSE async timeout 해제)
application-prod.yml   → 운영 환경 (Flyway ON, ddl-auto=validate)
application-local.yml  → 개인 로컬 (현재는 Flyway OFF / ddl-auto=update)  ※ 권장: dev 사용
application-example.yml→ 예시 템플릿
```

---

## ⚙️ Local / Dev 환경 준비
```text
0) MySQL / Redis 준비 (추천: Docker Compose)
프로젝트 루트에 docker-compose.yml가 포함되어 있습니다.
.env 생성 (예시는 .env.example 참고)
실행:
```
```bash
docker-compose up -d
```

```text
MySQL: 기본 포트는 .env의 MYSQL_PORT(예: 3307)
Redis: 6379
```
### 1) DB 생성 (직접 설치 MySQL 사용 시)

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
./gradlew bootRun --args='--spring.profiles.active=dev'
```
```text
application-dev.yml 기본값:
Flyway ON + baseline-on-migrate
JPA ddl-auto=validate
Redis 사용
SSE 끊김 방지: spring.mvc.async.request-timeout: -1
```

---

## 🔐 SSE 인증(브라우저) 안내
```text
브라우저 기본 EventSource는 Authorization 헤더를 붙이기 어렵습니다.
그래서 SSE는 HttpOnly 쿠키 기반 로그인을 지원합니다.
1) 로그인(쿠키 발급)
POST /api/auth/login-cookie
2) SSE 연결
GET /api/sse/subscribe
```
```javascript
// 1) 먼저 /api/auth/login-cookie로 로그인해서 쿠키가 세팅된 상태여야 함
const es = new EventSource("/api/sse/subscribe");

es.addEventListener("connected", (e) => console.log("connected", e.data));
es.addEventListener("ping", (e) => console.log("ping", e.data));
es.addEventListener("message-created", (e) => console.log("message", e.data));
es.addEventListener("message-deleted", (e) => console.log("deleted", e.data));
es.addEventListener("notification-created", (e) => console.log("noti", e.data));
```
```text
터미널(curl)로 빠른 확인:
```
```bash
# 1) 쿠키 발급
curl -i -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"email":"YOUR_EMAIL","password":"YOUR_PASSWORD"}' \
  http://localhost:8080/api/auth/login-cookie

# 2) SSE 구독
curl -N -b cookies.txt http://localhost:8080/api/sse/subscribe
```

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
  
이벤트 기반 처리
- 메시지/알림은 AFTER_COMMIT로 발행 → “DB 저장 성공한 것만” 후속 처리
```

---

## ⚡ Performance & DB Strategy

```scss
Cursor Pagination 적용
인덱스 최적화 (EXPLAIN 기반 검증)
denormalization(last message)로 목록/정렬 최적화
Flyway로 인덱스 변경까지 관리
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

Phase 1.1 — Security Hardening (추가 권장)
- Refresh Token 도입 (Access Token 단기화)
- 로그인 시도 제한 (IP / 계정 기준 rate limit)
- 비밀번호 변경 / 로그아웃 / 전체 로그아웃
- 권한 없는 리소스 접근 시 정보 노출 방지 (존재 여부 숨김 정책)
- 파일 업로드 확장자 / MIME 검증

---

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
- 중복 알림 방지 고도화(DB unique + 예외 처리)
- 알림 타입 확장(좋아요/댓글/팔로우)
- 읽지 않은 개수 캐싱 전략(Redis)

---

Phase 2.5 — Real-time (SSE) 안정화 ★ 중요
- Last-Event-ID 기반 재전송 완성 (오프라인 복구)
- Redis Pub/Sub → 다중 인스턴스 환경 검증
- Heartbeat / reconnect 정책 명확화
- 이벤트 저장소 TTL 정책 설계
- 알림/메시지 수신 순서 보장 전략
- 과도한 emitter 누수 방지(정리 스케줄러)

---

Phase 2.6 — Media Storage
- S3(또는 호환 스토리지) 업로드 전환
- 썸네일 생성(비동기)
- 이미지 메타데이터 제거(개인정보)
- 파일 접근 정책(공개/비공개 URL, signed URL)

---

Phase 2.7 — Feed & Performance
- 피드 N+1 제거 최적화
- 인기 게시글 랭킹 전략(좋아요/댓글/시간 decay)
- 조회수 정책(중복 조회 방지)
- Redis 캐시(핫 피드 / 프로필 카운트)

---

Phase 3 — Search
- 사용자 검색 (handle / name 기반)
  - prefix match + 정렬(정확 일치/접두 우선)
- 키워드 검색 (게시글/해시태그)
- 자동완성(Suggest)

Phase 3.1 — Advanced Search
- Elasticsearch/OpenSearch 도입
  - 한국어 분석기
  - 오타 교정 / 유사어
  - 통합 검색(유저/게시글/태그)
  - 랭킹 튜닝(인기/최신/관련도)
- 색인 동기화 전략
  - 이벤트 기반 + 재색인 배치

---

Phase 4 — DevOps / 운영
- Docker / Docker Compose (MySQL, Redis)
- Nginx Reverse Proxy
  - /api → Spring Boot
  - /docs → 정적 문서
  - gzip 압축 / 캐시 / 업로드 제한
- HTTPS (Let's Encrypt 자동 갱신)
- CI/CD (GitHub Actions: test → docs → build → deploy)

Phase 4.1 — Observability (운영 필수)
- 구조화 로그(JSON)
- MDC RequestId
- 요청 시간 / 슬로우 쿼리 로그
- Health check / readiness / liveness
- 메트릭(Prometheus + Grafana)
- 알림(Slack / Discord webhook)

---

Phase 5 — Product Expansion
- 공개 베타 서비스
- Android 앱 출시 (Google Play)
- iOS 앱 출시 (App Store)
- 개인정보 보호 / 약관 정비
- 사용자 신고 / 차단 기능
- 관리자(Admin) 백오피스

Phase 5.1 — Trust & Safety
- 스팸/도배 방지
- 신고 처리 플로우
- 유해 콘텐츠 대응 정책
```
