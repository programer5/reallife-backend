# OPERATIONS_CHECKLIST.md

RealLife 서비스 운영 안정화를 위한 **배포 전 / 운영 중 체크 문서**

이 문서는 다음 목적을 가진다.

- 배포 전 필수 체크
- 기능 회귀 테스트
- SSE / 인증 / 쿠키 안정성 확인
- 운영 대시보드 / 운영 알림 흐름 확인
- 문서 및 테스트 정합성 유지
- 장애 발생 시 확인 위치 안내

---

# 1. 배포 전 빌드 확인

## Backend build

```bash
./gradlew clean build
```

확인 사항

- build 실패 없음
- compile warning 과도하지 않음
- jar 정상 생성

```text
build/libs/backend-*.jar
```

## REST Docs 생성

```bash
./gradlew clean test asciidoctor
```

확인 사항

- `build/docs/asciidoc/index.html` 정상 생성
- include snippet 깨짐 없음
- controller 변경 후 docs 동기화 확인
- 아래 운영 스니펫 생성 확인
  - `admin-health-get`
  - `admin-health-realtime-get`
  - `admin-health-reminder-get`
  - `admin-dashboard-get`
  - `admin-errors-get`
  - `admin-alert-test-post`
  - `admin-alert-history-get`

---

# 2. 기능 회귀 테스트

운영 안정화를 위해 아래 기능은 **항상 배포 전 확인한다.**

## 인증

확인 항목

- 회원가입 정상 동작
- 로그인 정상 동작
- refresh cookie 발급
- refresh cookie 재발급

체크

- 로그인 후 새로고침 → 세션 유지
- 쿠키 삭제 → 인증 해제

## 피드

확인 항목

- 글 작성
- 글 수정
- 글 삭제
- 피드 목록 조회

체크

- 최신 글 정상 노출
- 작성 후 피드 즉시 반영

## 댓글

확인 항목

- 댓글 작성
- 댓글 reply
- 댓글 좋아요
- 댓글 목록

체크

- 댓글 수 증가
- 좋아요 수 증가
- 새로고침 후 정합성 유지

## Action 흐름

핵심 기능

```text
댓글
↓
Action 생성
↓
Conversation Dock
↓
Action Timeline
↓
Reminder
```

체크

- 댓글에서 Action 생성
- Action Dock 표시
- Conversation에서 Action 유지
- Reminder 생성

## 메시지

확인 항목

- 채팅 생성
- 메시지 전송
- 메시지 읽음 처리
- unread 표시

체크

- 메시지 전송 후 즉시 표시
- unread count 정상 감소

---

# 3. SSE 안정성 체크

RealLife는 다음 이벤트를 SSE로 전달한다.

```text
MESSAGE_RECEIVED
COMMENT_CREATED
POST_LIKED
PIN_CREATED
PIN_REMIND
```

확인 사항

- 새 메시지 SSE 수신
- 댓글 생성 SSE 수신
- 리마인더 SSE 수신
- 중복 이벤트 없음

추가 운영 체크

- `/admin/health/realtime`에서 active connection 증가 확인
- `lastSseEventSentAt` 갱신 확인
- 대시보드에서 realtime 상태가 DOWN / DEGRADED로 바뀌지 않는지 확인

---

# 4. Cookie / 인증 운영 체크

확인 항목

- refresh cookie 정상 발급
- SameSite 설정
- Secure 설정
- domain/path 확인

체크

브라우저 DevTools → Application → Cookies

확인

- refresh cookie 존재
- 만료시간 정상
- 로그인 유지

---

# 5. Flyway Migration 체크

확인

```text
src/main/resources/db/migration
```

체크

- migration 누락 없음
- checksum mismatch 없음
- 운영 알림 이력 테이블 관련 migration 정합성 유지

특히 확인할 것

- `ops_alert_log.id`는 UUID 기반 `binary(16)` 유지
- `ops_alert_log.body`는 `text` 유지
- 로그성 테이블이라고 해서 `bigint auto_increment`로 되돌리지 않기

문제 발생 시

```bash
flyway repair
```

과거 `V29__create_ops_alert_log.sql` 실패 이력이 남아 있으면 아래처럼 정리한다.

```sql
USE backend;
DROP TABLE IF EXISTS ops_alert_log;
DELETE FROM flyway_schema_history WHERE version = '29';
```

---

# 6. 운영 대시보드 / 운영 알림 체크

운영 점검 핵심 엔드포인트

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/admin/health`
- `/admin/health/realtime`
- `/admin/health/reminder`
- `/admin/dashboard`
- `/admin/errors`
- `/admin/alerts/test`
- `/admin/alerts/history`

배포 후 운영자가 가장 먼저 확인할 순서

1. `/ops/dashboard` 진입
2. 상단 상태 strip에서 DOWN / DEGRADED / FAILED 존재 여부 확인
3. FAILED alert pinned 영역 확인
4. 최근 서버 에러와 최근 운영 알림 이력 비교
5. realtime / reminder 상태 확인
6. 필요 시 Slack 테스트 알림 전송
7. 테스트 결과가 최근 운영 알림 이력에 쌓이는지 확인

Slack 테스트 알림 확인 기준

- 버튼 클릭 후 성공 응답 표시
- Slack 채널 수신 확인
- `/admin/alerts/history`에 최신 이력 반영 확인
- FAILED 이력이 남아 있으면 webhook / payload / cooldown 여부 점검

---

# 7. 프론트 운영자 진입 체크

현재 운영자 노출은 백엔드 role이 아니라 **프론트 Vite env allowlist** 기준이다.

확인 파일

```text
vue-front/.env.local
vue-front/.env.production
vue-front/src/router/index.js
vue-front/src/views/MeView.vue
```

확인 값

```env
VITE_OPS_ALLOWED_EMAILS=seed@test.com
VITE_OPS_ALLOWED_HANDLES=seed_001
```

체크

- 운영자 계정 이메일 또는 handle이 allowlist에 포함되어 있는지 확인
- 프론트 재시작 / 재빌드 후 `/me`에서 운영 도구 카드 노출 확인
- 비운영자 계정은 `/me?denied=ops`로 리다이렉트되는지 확인
- 설정 위치 안내 문구가 현재 계정 기준 예시값을 올바르게 보여주는지 확인

중요

- 백엔드 `.env`에 넣어도 프론트 운영자 노출에는 반영되지 않는다.
- `import.meta.env`로 읽기 때문에 빌드 산출물 교체가 필요하다.

---

# 8. 업로드 파일 관리

확인 항목

```text
uploads/
```

체크

- 이미지 업로드 정상
- 접근 URL 정상
- 오래된 테스트 파일이 운영 환경에 불필요하게 남아 있지 않은지 확인

---

# 9. Docker 상태 확인

컨테이너 상태

```bash
docker ps
```

예시

```text
reallife-nginx
reallife-app
reallife-redis
reallife-mysql
```

확인

- Restarting 상태 없음
- Health check 정상

---

# 10. 배포 후 체크

배포 후 아래 기능을 빠르게 확인한다.

- 로그인
- 피드 조회
- 글 작성
- 댓글 작성
- 메시지 전송
- SSE 이벤트 수신
- Reminder 알림
- `/ops/dashboard` 진입
- 최근 서버 에러 카드 표시
- 최근 운영 알림 이력 카드 표시
- Slack 테스트 알림 전송 및 이력 반영

---

# 11. 로그 확인

Backend

```bash
docker logs reallife-app
```

Nginx

```bash
docker logs reallife-nginx
```

추가 확인 포인트

- 서버 에러 발생 시 `GlobalExceptionHandler` 경유 알림 여부
- `OpsAlertService` cooldown 로그 여부
- health alert scheduler 관련 로그 여부
- Slack webhook 전송 실패 메시지 여부
