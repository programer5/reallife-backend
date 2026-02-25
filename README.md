# RealLife Backend

Spring Boot 기반 백엔드 서버입니다.  
현재는 **단일 모놀리식(monolith) + 모듈형 패키지 구조**로 개발하고 있으며, 기능이 안정화되면 **Kafka 기반 이벤트 드리븐 + MSA + Kubernetes 배포**로 확장하는 것을 목표로 합니다.

---

## 0) 운영 원칙 (현재 상황 / 비용 최소화 / 수익화 목표)

- 현재는 개발 단계이며 **도메인은 아직 구매하지 않았습니다.**
- 가능한 한 **무료/저비용 인프라를 우선**합니다. (무료 VPS/Free Tier 등)
- 실제 웹/앱으로 런칭 후에는 **운영비를 감당할 수 있도록 수익화가 필수**입니다.
  - 초기: 광고/간단한 유료 기능/후원 등 현실적인 방식부터
  - 중장기: 구독/프리미엄 기능/크리에이터 수익모델

---

## 1) Tech Stack (현재)

- **Java / Spring Boot**
  - Spring Web, Validation
  - Spring Security
  - Spring Data JPA
- **DB / Cache**
  - MySQL
  - Redis (Pub/Sub, 토큰/세션성 데이터)
- **Realtime**
  - SSE(Server-Sent Events)
  - Redis Pub/Sub 기반 멀티 인스턴스 Fan-out
- **DB Migration**
  - Flyway
- **API Docs**
  - Spring REST Docs (Asciidoctor)
- **Infra (로컬/서버)**
  - Docker / Docker Compose
  - Nginx Reverse Proxy (프론트 정적서빙 + `/api` 프록시 + `/docs` 서빙)

---

## 2) 주요 기능 (현재)

### Auth / Account
- 회원가입
- 로그인(토큰 발급)
- Refresh Token Rotation(재발급 토큰 회전)
- Refresh Token 재사용 감지/차단
- 전 기기 로그아웃(Logout-All)
- 내 정보 조회: `GET /api/me`

#### ✅ Cookie 기반 인증 (브라우저/SSE 권장)
브라우저 SPA + SSE를 고려할 때 **HttpOnly 쿠키 기반 인증**을 권장합니다.

- `POST /api/auth/login-cookie`
  - `access_token`(HttpOnly) / `refresh_token`(HttpOnly) Set-Cookie
- `POST /api/auth/refresh-cookie`
  - refresh_token 쿠키로 access/refresh 재발급
- `POST /api/auth/logout-cookie`
- `POST /api/auth/logout-all-cookie`

> 프론트(axios)에서는 `withCredentials: true` + 401 발생 시 `refresh-cookie` 호출 후 원요청 1회 재시도(무한루프 방지) 패턴을 권장합니다.

#### Bearer 방식(대안)
모바일 앱/서버-서버 호출 등 “쿠키를 쓰기 애매한 환경”에서 사용합니다.

- `POST /api/auth/login` → accessToken 발급
- 요청 헤더: `Authorization: Bearer <accessToken>`

---

### Users / Social
- 사용자 검색(커서 페이징)
- 팔로우/언팔로우
- 프로필 조회 (핸들 기반)

---

### Posts / Feed
- 게시글 생성/조회/삭제
- 피드 조회(커서 페이징)
- 댓글 생성/목록/삭제(커서 페이징)
- 좋아요/좋아요 취소

#### 게시글 이미지 저장 정책(중요)
- **정석 방식(권장)**: `imageFileIds: [UUID]`
  1) 파일 업로드 → 응답의 `id`(UploadedFile ID) 획득
  2) 게시글 생성 요청에 `imageFileIds`로 전달
- **구버전 호환**: `imageUrls: [String]` (점진 전환용)

파일 서빙:
- `GET /api/files/{fileId}/download` (**브라우저에서 바로 렌더링 가능**)

> `<img src="...">`는 Authorization 헤더를 넣기 어렵기 때문에, `/download`는 브라우저 직접 접근(헤더 없이)도 가능하도록 설계되어 있습니다.  
> 추후 “비공개 계정/권한”이 필요해지면 Signed URL 방식으로 고도화할 수 있습니다.

---

### DM / Messages
- DIRECT 대화방 생성/조회(Idempotent)
- 대화방 목록(커서 페이징)
- 메시지 목록(커서 페이징)
- 메시지 자동 읽음 처리(last_read_at 갱신)
- 메시지 삭제(나만 삭제/모두 삭제)

---

### Notifications / Realtime(SSE)
- 알림 목록(커서 페이징)
- 알림 단건 읽음/전체 읽음
- 읽은 알림 일괄 삭제(soft delete)
- SSE 구독: `GET /api/sse/subscribe`
  - 이벤트: `connected`, `ping`, `message-created`, `notification-created`

---

## 3) 로컬 실행 (Docker Compose)

### 3-1) 환경 파일 준비
`.env.example`을 복사해 `.env`를 생성하세요.

```bash
cp .env.example .env
```

필수로 확인/수정 권장 값:
- `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`
- `JWT_SECRET` (길고 랜덤한 값)
- `JWT_ACCESS_TOKEN_EXP_MINUTES`, `JWT_REFRESH_TOKEN_EXP_DAYS`

> ⚠️ `.env`는 **절대 커밋하지 않습니다.** (아래 “Git 체크” 참고)

### 3-2) 실행
```bash
docker compose down
docker compose up -d --build
docker compose ps
```

### 3-3) 접속
| 항목 | 주소 |
|---|---|
| Docs (Nginx) | http://localhost/docs |
| API | http://localhost/api |
| SSE | http://localhost/api/sse/subscribe |
| File download | http://localhost/api/files/{fileId}/download |

---

## 4) Postman 테스트 가이드 (추천 흐름)

### 4-1) 로그인 → 토큰 확보(Bearer)
- `POST {{baseUrl}}/api/auth/login`
- 응답의 `accessToken`을 복사

### 4-2) 파일 업로드
- `POST {{baseUrl}}/api/files`
- Authorization: `Bearer {{accessToken}}`
- Body: form-data
  - key: `file` (type: File)

응답 예시:
```json
{
  "id": "UUID",
  "url": "/api/files/UUID/download",
  "originalFilename": "cat.png",
  "contentType": "image/png",
  "size": 12345
}
```

### 4-3) 게시글 생성 (정석: imageFileIds)
- `POST {{baseUrl}}/api/posts`
- Authorization: `Bearer {{accessToken}}`
- Body(JSON):
```json
{
  "content": "hello",
  "imageFileIds": ["업로드 응답의 id"],
  "visibility": "PUBLIC"
}
```

---

## 5) API 문서 (Spring REST Docs)

Spring REST Docs는 다음 흐름으로 동작합니다:

1) 테스트 실행 시 `document("snippet-id")`로 스니펫 생성  
2) `build/generated-snippets/` 아래에 스니펫 폴더 생성  
3) `src/docs/asciidoc/index.adoc`가 스니펫을 include  
4) asciidoctor가 HTML 생성

로컬에서 문서 생성:
```bash
./gradlew clean test asciidoctor
```

- 스니펫: `build/generated-snippets/`
- 결과: `build/docs/asciidoc/index.html`

---

## 6) SSE 운영 팁 (Nginx/Proxy)

SSE는 연결을 오래 유지하므로 프록시 설정이 중요합니다.

권장(요약):
- `proxy_buffering off;` (버퍼링 비활성화)
- `proxy_read_timeout 3600s;` (충분히 크게)
- `X-Accel-Buffering: no` 헤더

---

## 7) Git 체크 (실서비스/오픈소스 안전)

✅ 커밋 금지(레포에 포함되면 안 됨):
- `.env`
- `uploads/`
- `build/`
- `src/main/resources/application-*.yml` 중 dev/prod/local 같이 비밀값 들어가는 파일
- `.idea/`, `.vscode/`, `*.log`

예시 `.gitignore`:
```gitignore
.env
uploads/
build/
*.log
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
src/main/resources/application-local.yml
```

이미 커밋된 경우(추적 제거):
```bash
git rm --cached src/main/resources/application-dev.yml
git rm --cached src/main/resources/application-prod.yml
git rm --cached src/main/resources/application-local.yml
git rm --cached .env -f
git rm --cached -r uploads build
```

---

## 8) CI / Docs / CD

- CI: 테스트/문서 생성
- Docs: `build/docs/asciidoc/`을 배포 (GitHub Pages 등)

CD(실서버 배포)는 Docker Compose 기반으로 단계를 나눠서 추가하는 것을 권장합니다.

---

## 9) Kafka / Event-Driven / MSA 확장 계획 (추가 예정)

지금은 “기능 완성 + 데이터 모델 안정화”가 1순위입니다.  
그 다음 Kafka + Outbox 패턴 기반으로 이벤트 발행/소비 후 점진적 서비스 분리를 권장합니다.

---

## 10) Roadmap (추천 순서)
- [ ] 프로필 조회/수정 + 프로필 이미지
- [ ] 게시글 이미지 썸네일/리사이즈 파이프라인(비동기 처리 포함)
- [ ] 검색 고도화 (DB 인덱스/쿼리) → (선택) Elasticsearch/OpenSearch Read Model
- [ ] CD 추가(서버 배포 자동화) → 이후 K8s 전환
- [ ] Kafka 도입: Outbox + 도메인 이벤트 발행/소비
- [ ] Notification/Search/Feed 등 점진적 서비스 분리(MSA)
- [ ] HTTPS 실배포(도메인/서버 구성)

---

## 11) License
필요 시 라이선스를 명시하세요.

---

## 12) Product Roadmap (추천 순서)

### Phase 1 — 프론트 완성도(UX) 마감과 병렬로 “백엔드 안정화”
- [ ] 권한/검증(모든 write API: owner check / visibility check)
- [ ] Rate limit / Abuse 방어 (로그인, 검색, DM 전송, 파일 업로드, SSE)
- [ ] SSE 안정화
  - reconnect/Last-Event-ID 유실 대비
  - 다중 인스턴스 fan-out(현재 Redis Pub/Sub) 동작 검증
  - 프록시 설정(Nginx buffering off, timeouts) 점검
- [ ] 테스트 확충
  - 인증/권한/핵심 도메인(게시글/댓글/DM/알림) 컨트롤러 테스트
  - REST Docs 스니펫 최신화
- [ ] 로깅/모니터링
  - 에러 로그 구조화, 요청 ID, 기본 메트릭(응답시간/에러율)

### Phase 2 — 차별화 1~2개 먼저 출시 (추천)
**추천 1) 락 메시지(비밀번호 잠금)**
- 메시지 엔티티 확장(locked, pinHash, viewLimit, expireAt 등)
- 잠금 해제 API + 서버 검증
- 열람 이벤트 기록(횟수 제한/감사 로그)

**추천 2) 약속/장소 자동 넛지**
- 메시지 내용에서 시간/장소 패턴 감지(초기엔 규칙 기반)
- conversation 상단 pin 저장 + 알림 스케줄링(예: 1시간 전)
- 추후 캘린더 연동/투표/지도 추천으로 확장

### Phase 3 — AI 기능은 옵션으로 확장
- Catch-up 요약 / To-do 추출
- 비용/정확도를 위해 “요약 생성 → 사용자 확정/저장” 플로우 권장
- 비동기 처리(큐) + 캐시 전략 고려

---

## 13) Differentiation Ideas (경쟁력 기능 후보)

### A. 실사용 락인 기능
- 약속/장소 자동 넛지(채팅 상단 핀 + 알림)
- 대화 내 검색(키워드+필터) + 중요 메시지 북마크
- 타임캡슐 메시지(날짜/장소 조건 해제)

### B. 시그니처(바이럴/재방문)
- 락 메시지(비밀번호 잠금)
  - PIN/생체/비번 잠금(환경에 맞게)
  - 시간/횟수 제한
- 감정 이펙트(키워드 기반 UI 효과)
  - 서버는 이벤트 타입만 제공하고, 연출은 프론트에서 처리(가성비)

### C. AI(선택형)
- Catch-up 카드 + To-do 추출(확정 버튼형)
- 자동 등록보다 사용자 확정 UX 추천

---

## 14) Release Checklist (백엔드)
- [ ] CORS/쿠키/SameSite/HTTPS 설정 점검(실서비스 필수)
- [ ] 파일 업로드 제한(확장자/용량/바이러스 스캔은 선택)
- [ ] 인덱스 점검(피드/검색/DM 목록/알림)
- [ ] DB 백업/복구 계획(최소 스냅샷)
- [ ] 운영 환경 분리(local/dev/prod) 및 비밀값 관리
