# RealLife Backend

Spring Boot 기반 백엔드 서버입니다.  
현재는 **단일 모놀리식(monolith) + 모듈형 패키지 구조**로 개발하고 있으며, 기능이 안정화되면 **Kafka 기반 이벤트 드리븐 + MSA + Kubernetes 배포**로 확장하는 것을 목표로 합니다.

> 이 저장소는 **백엔드 API / 실시간(SSE) / 문서화(REST Docs) / 컨테이너 실행**을 중심으로 관리합니다.

---

## 1) Tech Stack (현재)

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
- **Infra (로컬)**
  - Docker / Docker Compose
  - Nginx Reverse Proxy

---

## 2) 주요 기능 (현재)

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
  1) 파일 업로드 → 응답의 `id`(UploadedFile ID) 획득
  2) 게시글 생성 요청에 `imageFileIds`로 전달
- **구버전 호환**: `imageUrls: [String]` (점진 전환용)

파일 서빙:
- `GET /api/files/{fileId}/download` (**브라우저에서 바로 렌더링 가능**)

> `<img src="...">`는 Authorization 헤더를 넣기 어렵기 때문에, `/download`는 브라우저 직접 접근(헤더 없이)도 가능하도록 설계되어 있습니다.  
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

---

## 5) API 문서(REST Docs)

문서 생성:
```bash
./gradlew clean test asciidoctor copyRestDocs
```

문서 확인:
- `http://localhost/docs`

---

## 6) Git 체크(실서비스/오픈소스 안전)

### 6-1) 커밋 금지 파일
- `.env`, `uploads/`, `build/`, 개인 설정 yml(`application-*.yml` 중 dev/prod/local 등)

`.gitignore` 예시:
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

## 7) Kafka / Event-Driven / MSA 확장 계획 (추가 예정)

> 지금은 “기능 완성 + 데이터 모델 안정화”가 1순위입니다.  
> 그 다음 **Kafka를 이용해 도메인 이벤트를 발행하고**, 점진적으로 서비스 분리를 진행합니다.  
> (처음부터 MSA로 시작하면 개발 속도/디버깅 비용이 급격히 증가할 수 있어, **단계적 전환**을 권장합니다.)

### 7-1) 도입 목표
- 서비스 간 결합도를 낮추고(비동기 이벤트), 확장성과 장애 격리를 높입니다.
- “게시글 생성 → 알림/피드/검색 인덱싱” 같은 후처리를 이벤트로 분리합니다.

### 7-2) 이벤트 설계(초안)
- `user.created`
- `follow.created`, `follow.deleted`
- `post.created`, `post.deleted`
- `comment.created`
- `like.created`, `like.deleted`
- `dm.message.created`
- `notification.created`

> 이벤트는 **Outbox 패턴**으로 발행(추천)  
> - DB 트랜잭션과 이벤트 발행의 정합성 확보  
> - 최소 1회 전달(at-least-once) 전제 + 멱등성(idempotency) 처리

### 7-3) 점진적 서비스 분리 후보(추천 순서)
1) **Notification Service** (이벤트 소비 → 알림 저장/전송)
2) **Search/Indexing Service** (Elasticsearch/OpenSearch 인덱싱 전담)
3) **Feed Service** (피드 생성/랭킹)
4) **Media Service** (썸네일/리사이즈/스토리지)

> 핵심 도메인(Post/User/Auth)은 마지막까지 모놀리식에 남겨두는 전략이 안정적입니다.

### 7-4) 로컬 Kafka 구성(예정)
- Docker Compose에 `kafka` + `zookeeper`(또는 KRaft) 추가
- Spring Kafka 의존성 추가
- Outbox 테이블 + 퍼블리셔 + 컨슈머 모듈 추가

> 실제 추가 시: `docker-compose.kafka.yml` 같은 별도 파일로 분리하는 것을 권장합니다.

---

## 8) Kubernetes(K8s) 배포 계획 (추가 예정)

### 8-1) 목표
- Nginx Ingress + TLS(HTTPS)
- API 서버 다중 레플리카 + 오토스케일(선택)
- ConfigMap/Secret로 설정 분리
- Observability(로그/지표/트레이싱) 기반 운영

### 8-2) 예상 구성(초안)
- `Deployment` : reallife-api
- `Service` : ClusterIP
- `Ingress` : 외부 라우팅(도메인/HTTPS)
- `ConfigMap/Secret` : 환경변수/민감정보
- `HPA` : 트래픽 기반 스케일(선택)

### 8-3) 디렉터리 구조(예정)
- `k8s/base/` (deployment/service/ingress)
- `k8s/overlays/dev`, `k8s/overlays/prod` (kustomize)
- 또는 `helm/` 차트로 관리

---

## 9) Roadmap (추천 순서)

- [ ] 프로필 조회/수정 + 프로필 이미지
- [ ] 게시글 이미지 썸네일/리사이즈 파이프라인(비동기 처리 포함)
- [ ] 검색 고도화 (DB 인덱스/쿼리) → (선택) Elasticsearch/OpenSearch Read Model
- [ ] Kafka 도입: Outbox + 도메인 이벤트 발행/소비
- [ ] Notification/Search/Feed 등 점진적 서비스 분리(MSA)
- [ ] Kubernetes 배포(ingress/https/secret/config)
- [ ] Vue.js 프론트엔드
- [ ] HTTPS 실배포(도메인/서버 구성)

---

## 10) License
필요 시 라이선스를 명시하세요.
