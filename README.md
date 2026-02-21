# RealLife Backend

Spring Boot 기반 백엔드 서버입니다.  
현재는 **단일 모놀리식(monolith) + 모듈형 패키지 구조**로 개발하고 있으며, 기능이 안정화되면 **Kafka 기반 이벤트 드리븐 + MSA + Kubernetes 배포**로 확장하는 것을 목표로 합니다.

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
| Docs (Nginx) | http://localhost/docs |
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

로컬에서 문서 생성:
```bash
./gradlew clean test asciidoctor
```

- 결과: `build/docs/asciidoc/`

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

## 7) CI / Docs / CD

이 레포는 GitHub Actions 기반으로 **CI(테스트) + Docs 배포(REST Docs → GitHub Pages)** 가 이미 구성되어 있습니다.

### 7-1) CI (현재)
- `.github/workflows/ci.yml`
  - Redis 서비스 컨테이너를 띄운 뒤
  - `SPRING_PROFILES_ACTIVE=test`로 테스트 실행
  - `./gradlew clean test asciidoctor`
  - 생성된 REST Docs 결과물을 Artifact로 업로드

### 7-2) Docs 배포 (현재)
- `.github/workflows/docs.yml`
  - main 브랜치 push 또는 수동 실행(`workflow_dispatch`) 시
  - `./gradlew clean test asciidoctor`
  - `build/docs/asciidoc/`을 GitHub Pages로 배포

> GitHub Pages URL은 레포 Settings → Pages에서 확인할 수 있습니다.

### 7-3) CD (추가 권장)
현재 워크플로는 “빌드/테스트/문서” 중심이며, **실서버 배포(CD)** 단계는 별도로 두는 것을 추천합니다.

#### CD 1단계(추천): Docker 이미지 빌드 → 서버에서 docker compose로 배포
1) GitHub Actions에서 Docker 이미지를 빌드해서 **GHCR(ghcr.io)** 로 push
2) 서버(VPS)에서 `docker compose pull && docker compose up -d`로 반영

필요 GitHub Secrets(예시):
- `GHCR_TOKEN` (또는 `GITHUB_TOKEN`로 대체 가능)
- `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_PATH`

(복붙용) `cd.yml` 예시:
```yaml
name: CD
on:
  push:
    branches: [ "main" ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: gradle

      - name: Build
        run: ./gradlew clean bootJar

      - name: Login to GHCR
        run: echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin

      - name: Build & Push image
        run: |
          IMAGE=ghcr.io/${{ github.repository_owner }}/reallife-api:latest
          docker build -t $IMAGE .
          docker push $IMAGE

      - name: Deploy via SSH (docker compose pull/up)
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd ${{ secrets.DEPLOY_PATH }}
            docker compose pull
            docker compose up -d
            docker image prune -f
```

#### CD 2단계: Kubernetes로 이전
- 이미지 빌드/푸시는 동일
- 배포 단계에서 `kubectl apply -k k8s/overlays/prod` 또는 Helm upgrade 실행

> 처음부터 K8s로 가기보다, **Docker Compose CD로 운영 흐름을 만든 뒤** K8s로 이전하는 것이 학습/운영 모두에 유리합니다.

---

## 8) Kafka / Event-Driven / MSA 확장 계획 (추가 예정)

> 지금은 “기능 완성 + 데이터 모델 안정화”가 1순위입니다.  
> 그 다음 **Kafka를 이용해 도메인 이벤트를 발행하고**, 점진적으로 서비스 분리를 진행합니다.  
> (처음부터 MSA로 시작하면 개발 속도/디버깅 비용이 급격히 증가할 수 있어, **단계적 전환**을 권장합니다.)

### 8-1) 도입 목표
- 서비스 간 결합도를 낮추고(비동기 이벤트), 확장성과 장애 격리를 높입니다.
- “게시글 생성 → 알림/피드/검색 인덱싱” 같은 후처리를 이벤트로 분리합니다.

### 8-2) 이벤트 설계(초안)
- `user.created`
- `follow.created`, `follow.deleted`
- `post.created`, `post.deleted`
- `comment.created`
- `like.created`, `like.deleted`
- `dm.message.created`
- `notification.created`

> 이벤트는 **Outbox 패턴**으로 발행(추천)

### 8-3) 점진적 서비스 분리 후보(추천 순서)
1) Notification Service
2) Search/Indexing Service
3) Feed Service
4) Media Service(썸네일/리사이즈/스토리지)

---

## 9) Kubernetes(K8s) 배포 계획 (추가 예정)

### 9-1) 목표
- Nginx Ingress + TLS(HTTPS)
- API 서버 다중 레플리카 + 오토스케일(선택)
- ConfigMap/Secret로 설정 분리

### 9-2) 디렉터리 구조(예정)
- `k8s/base/`
- `k8s/overlays/dev`, `k8s/overlays/prod` (kustomize)
- 또는 `helm/`

---

## 10) Roadmap (추천 순서)
- [ ] 프로필 조회/수정 + 프로필 이미지
- [ ] 게시글 이미지 썸네일/리사이즈 파이프라인(비동기 처리 포함)
- [ ] 검색 고도화 (DB 인덱스/쿼리) → (선택) Elasticsearch/OpenSearch Read Model
- [ ] CD 추가(서버 배포 자동화) → 이후 K8s 전환
- [ ] Kafka 도입: Outbox + 도메인 이벤트 발행/소비
- [ ] Notification/Search/Feed 등 점진적 서비스 분리(MSA)
- [ ] Vue.js 프론트엔드
- [ ] HTTPS 실배포(도메인/서버 구성)

---

## 11) License
필요 시 라이선스를 명시하세요.