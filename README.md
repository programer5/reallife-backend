# 📸 RealLife (Instagram-style SNS Backend)

> Real moments. Real people. Real life.

**Spring Boot 기반 SNS 백엔드 프로젝트**입니다.  
현재 코드 베이스는 **JWT 인증(Refresh Rotation) + REST Docs + Flyway + Redis Pub/Sub + SSE + Docker(Nginx/MySQL/Redis/App)** 까지 갖춘 “실서비스에 가까운 뼈대”를 목표로 구성되어 있습니다.

## 1) 현재 구현된 기능

### ✅ 인증 / 보안
- Stateless JWT 인증
- Refresh Rotation(재발급 토큰 회전)
- Logout-All(전 기기 로그아웃) 지원
- `/api/me` 내 정보 조회 API

### ✅ 유저 / 소셜
- 회원가입
- 사용자 검색(커서 페이징)
- 팔로우/언팔로우
- (User 엔티티에 followerCount 유지)

### ✅ 콘텐츠
- 게시글 생성 / 조회 / 피드 조회(커서 페이징)
- 댓글 생성 / 목록 / 삭제
- 좋아요 / 좋아요 취소

### ✅ 실시간 / 메시징
- DM(대화방 생성/조회, 메시지 목록, 자동 읽음 처리)
- SSE 구독 (`/api/sse/subscribe`)
- Redis Pub/Sub 기반 멀티 인스턴스 SSE Fanout
- 알림 시스템(커서 페이징 / 읽음 처리 / 일괄 삭제)

### ✅ 문서 / 운영
- Spring REST Docs 자동 문서화
- Docker Compose: MySQL + Redis + App + Nginx

---

## 2) 빠른 실행(로컬)

### 2-1) 환경파일 준비
프로젝트에 있는 파일은 `.env.example` 입니다.

```bash
cp .env.example .env
```

### 2-2) 전체 실행
```bash
docker compose down
docker compose up -d --build
```

### 2-3) 접속
| 기능 | 주소 |
|---|---|
| API | http://localhost:8080/api |
| Docs | http://localhost:8080/docs |
| SSE | http://localhost:8080/api/sse/subscribe |

---

## 3) API 문서(REST Docs) 생성

```bash
./gradlew clean test asciidoctor copyRestDocs
```

---

## 4) SSE 이벤트

- `connected`
- `ping`
- `message-created`
- `notification-created`

---

### A. 사용자 프로필
- [ ] 프로필 조회: `GET /api/users/{handle}` (혹은 id)
- [ ] 프로필 수정: 이름/소개(bio)/웹사이트/프로필 이미지
- [ ] followerCount뿐 아니라 **followingCount / postCount** 관리(집계 전략 학습)

> ✅ 추천 기술 포인트  
> - 프로필/집계는 “정규화 vs 비정규화(카운트 컬럼)” 트레이드오프를 경험하기 좋음  
> - 동시성/정합성(락/이벤트/배치) 같은 주제를 자연스럽게 배우게 됨

### B. 미디어(사진) 파이프라인
현재는 `PostCreateRequest.imageUrls`가 문자열 리스트이고, 파일 업로드는 `/api/files`가 존재하지만 **일반 다운로드/서빙 정책이 부족**합니다.

- [ ] 업로드 파일을 “조회 가능한 URL”로 서빙 (예: `/api/files/{id}/download` 또는 `/uploads/{key}`)
- [ ] 이미지 타입/용량 검증 강화 (MIME 스니핑, 확장자/콘텐츠 불일치 방지)
- [ ] 이미지 리사이즈/썸네일 생성(예: 1080px, 320px) + 원본 보관
- [ ] 스토리지 추상화 확장: Local → (학습용) S3 호환 스토리지로 교체 가능하게

> ✅ 추천 기술 포인트  
> - (학습) LocalStorage → StorageService(S3 구현)로 확장  
> - 썸네일 생성은 서버에서 처리(비동기)하거나, 프론트에서 리사이즈 후 업로드하는 방식도 비교 가능

### C. 게시글/피드 고도화
- [ ] 게시글 수정/숨김/아카이브/삭제 정책(Soft delete)
- [ ] 피드 랭킹(최신순 → “친밀도/가중치” 실험 가능)
- [ ] 게시글 목록: 내 게시글, 특정 유저 게시글, 좋아요한 게시글
- [ ] 댓글/좋아요 “목록 조회” (누가 좋아요 했는지 / 누가 댓글 달았는지)

> ✅ 추천 기술 포인트  
> - 커서 페이징을 이미 적용했으니, “정렬키 설계(createdAt + id)”를 더 세련되게 다듬기 좋음

### D. 탐색/검색
- [ ] 해시태그(파싱/저장/검색)
- [ ] 멘션(@user) 파싱 및 알림 연동
- [ ] 인기글/추천유저(간단한 스코어링)

> ✅ 추천 기술 포인트  
> - 처음에는 DB 인덱스 + LIKE/Prefix 검색으로 시작 → 나중에 Elasticsearch/OpenSearch로 확장(선택)

### E. 안전/프라이버시(실서비스급)
- [ ] 차단(Block) / 뮤트(Mute)
- [ ] 비공개 계정(팔로우 승인 흐름)
- [ ] 신고(Report) / 콘텐츠 숨김
- [ ] Rate limiting(로그인/검색/댓글 폭주 방지)

> ✅ 추천 기술 포인트  
> - Redis 기반 Rate limit(토큰 버킷) 같은 패턴을 학습하기 좋음

### F. 운영/관측성(나중에 배포 때 진짜 도움됨)
- [ ] Actuator + Prometheus 지표
- [ ] 로그 상관관계(RequestId) 유지
- [ ] 에러 응답 표준화(이미 어느 정도 되어 있다면 문서화 강화)
- [ ] CI: 테스트 + 문서 생성 자동화(GitHub Actions)

---

## 6) 백엔드 완료 체크리스트

프론트를 들어가기 전에 아래 6개만 안정적으로 되면, 화면 만들 때 속도가 확 올라갑니다.

1. **Auth 흐름이 안정적**: 로그인/재발급/만료/로그아웃-all
2. **프로필**: 조회/수정 + 프로필 이미지
3. **미디어 서빙**: 업로드 → 접근 URL → 게시글 이미지로 표시
4. **피드/게시글 목록**: 커서 페이징 정합성 + 삭제/숨김 정책
5. **알림/DM 실시간**: SSE/Redis fanout 안정성(끊김 복구 포함)
6. **문서/테스트**: REST Docs가 실제 API와 동일하게 유지

---

## 7) Roadmap

- [x] 인증 / 사용자 / 검색
- [x] DM + 알림 + SSE (Redis Fanout)
- [x] REST Docs / Docker Compose
- [ ] **프로필(조회/수정) + 프로필 이미지**
- [ ] **파일 다운로드/서빙 + 이미지 썸네일**
- [ ] 해시태그/멘션 파싱 + 알림
- [ ] 피드 랭킹(최신순 → 가중치) / Explore
- [ ] 차단/비공개/신고 + Rate limit
- [ ] Vue.js 프론트엔드
- [ ] HTTPS 배포

---

## 8) 개발 규칙

- 기능 추가할 때마다:
  1) 테스트 작성 → 2) 구현 → 3) REST Docs 스니펫 생성 → 4) 문서 반영
- “작게 완성하고 다음으로”: 한 번에 크게 만들면 리팩토링이 지옥이 됩니다 😅
