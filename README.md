# RealLife Backend

Spring Boot 기반 백엔드 서버입니다.  
**JWT 인증(Refresh Rotation) + REST Docs + Flyway + Redis Pub/Sub + SSE + Docker Compose(MySQL/Redis/Nginx/App)** 구성을 갖추고 있습니다.

> 이 저장소는 **백엔드 API / 실시간(SSE) / 문서화(REST Docs) / 컨테이너 실행**을 중심으로 관리합니다.

---

## 1) Tech Stack

- **Java / Spring Boot**
  - Spring Web, Validation
  - Spring Security (JWT)
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
- **Infra**
  - Docker / Docker Compose
  - Nginx Reverse Proxy

---

## 2) 주요 기능

### Auth / Account
- 회원가입
- 로그인(Access Token 발급)
- Refresh Token Rotation(재발급 토큰 회전)
- Refresh Token 재사용 감지/차단
- 전 기기 로그아웃(Logout-All)
- 내 정보 조회: `GET /api/me`

### Users / Social
- 사용자 검색(커서 페이징)
- 팔로우/언팔로우

### Posts / Feed
- 게시글 생성/조회/삭제
- 피드 조회(커서 페이징)
- 댓글 생성/목록/삭제(커서 페이징)
- 좋아요/좋아요 취소

#### 게시글 이미지 저장 정책(중요)
- **정석 방식(권장)**: `imageFileIds: [UUID]`  
  - 먼저 파일 업로드 → 응답의 `id`(UploadedFile ID)를 게시글 생성 요청에 전달
- **구버전 호환**: `imageUrls: [String]`  
  - 당장 프론트를 바꾸기 어렵다면 유지 가능 (점진 전환용)

서버는 게시글 응답에서 `imageUrls`를 내려주며, 파일 서빙 URL 형태는 다음과 같습니다.

- `GET /api/files/{fileId}/download`  (**브라우저에서 바로 렌더링 가능**)

> 참고: `<img src="...">`에서 Authorization 헤더를 넣을 수 없기 때문에,  
> `/download`는 브라우저 직접 접근(헤더 없이)도 가능하도록 설계되어 있습니다.  
> 추후 “비공개 계정/권한”이 필요해지면 Signed URL 방식으로 고도화할 수 있습니다.

### DM / Messages
- DIRECT 대화방 생성/조회(Idempotent)
- 대화방 목록(커서 페이징)
- 메시지 목록(커서 페이징)
- 메시지 자동 읽음 처리(last_read_at 갱신)
- 메시지 삭제(나만 삭제/모두 삭제)

### Notifications / Realtime(SSE)
- 알림 목록(커서 페이징)
- 알림 단건 읽음/전체 읽음
- 읽은 알림 일괄 삭제(soft delete)
- SSE 구독: `GET /api/sse/subscribe`
  - 이벤트: `connected`, `ping`, `message-created`, `notification-created`

---

## 3) 로컬 실행 (Docker Compose)

### 3-1) 사전 준비
- Docker / Docker Compose 설치

### 3-2) 환경 파일 준비
`.env.example`을 복사해 `.env`를 생성하세요.

```bash
cp .env.example .env
```

필수로 확인/수정 권장 값:
- `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`
- `JWT_SECRET` (길고 랜덤한 값)
- `JWT_ACCESS_TOKEN_EXP_MINUTES`, `JWT_REFRESH_TOKEN_EXP_DAYS`

> ⚠️ `.env`는 **절대 커밋하지 않습니다.** (아래 “Git 체크” 참고)

### 3-3) 실행
```bash
docker compose down
docker compose up -d --build
docker compose ps
```

### 3-4) 접속
| 항목 | 주소 |
|---|---|
| Docs | http://localhost/docs |
| API | http://localhost/api |
| File download | http://localhost/api/files/{fileId}/download |

---

## 4) Postman 테스트 가이드(추천 흐름)

### 4-1) 로그인 → 토큰 확보
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

응답의 `imageUrls[0]`를 브라우저에서 열면 이미지가 렌더링됩니다:
- `http://localhost` + `/api/files/{id}/download`

---

## 5) API 문서(REST Docs)

### 5-1) 문서 생성
```bash
./gradlew clean test asciidoctor copyRestDocs
```

### 5-2) 문서 확인
- `http://localhost/docs`

> REST Docs 스니펫은 테스트 실행 시 `build/generated-snippets`에 생성됩니다.

---

## 6) Git 체크(실서비스/오픈소스 안전)

### 6-1) 커밋 금지 파일
다음 항목은 **절대 커밋하지 않도록** 관리합니다.

- `.env`
- `uploads/` (로컬 파일 저장소)
- `build/`
- `application-local.yml`, `application-dev.yml` 등 개인 설정

`.gitignore` 예시:
```gitignore
.env
uploads/
build/
*.log
src/main/resources/application-local.yml
src/main/resources/application-dev.yml
```

> 이미 커밋된 경우:
```bash
git rm --cached .env -f
git rm --cached -r uploads build
```

---

## 7) Roadmap (추천 순서)

- [ ] 프로필 조회/수정 + 프로필 이미지
- [ ] 게시글 이미지 썸네일/리사이즈 파이프라인(비동기 처리 포함)
- [ ] 검색 고도화 (DB 인덱스/쿼리) → (선택) Elasticsearch/OpenSearch Read Model
- [ ] 비공개/차단/신고 + Rate limit 확장
- [ ] 관측성(Actuator/지표/로그 상관관계)
- [ ] Vue.js 프론트엔드
- [ ] HTTPS 실배포(도메인/서버 구성)

---

## 8) License
필요 시 라이선스를 명시하세요.
