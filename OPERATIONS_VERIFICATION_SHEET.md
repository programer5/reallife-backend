# RealLife 운영 검증 시트 (Operations Verification Sheet)

배포 전 / 운영 점검 시 실제 결과 기록용

---

# 1. 빌드 / 테스트 / 문서

| 항목 | 확인 방법 | 결과 | 비고 |
|-----|----------|------|------|
| Gradle build 성공 | `./gradlew clean build` | PASS / FAIL | |
| 전체 테스트 통과 | `./gradlew test` | PASS / FAIL | |
| REST Docs 생성 | `./gradlew asciidoctor` | PASS / FAIL | |
| generated-snippets 생성 | `build/generated-snippets` 확인 | PASS / FAIL | |
| docs html 생성 | `build/docs/asciidoc/index.html` | PASS / FAIL | |

---

# 2. REST Docs 스니펫 확인

| Endpoint | Snippet 폴더 | 결과 |
|---------|--------------|------|
| admin-health-get | generated-snippets/admin-health-get | PASS / FAIL |
| admin-health-realtime-get | generated-snippets/admin-health-realtime-get | PASS / FAIL |
| admin-health-reminder-get | generated-snippets/admin-health-reminder-get | PASS / FAIL |
| users-exists | generated-snippets/users-exists | PASS / FAIL |
| users-profile-get-by-id | generated-snippets/users-profile-get-by-id | PASS / FAIL |

---

# 3. Health Endpoint 점검

사전 조건: `OPS_ADMIN_ALLOWED_EMAILS` 또는 `OPS_ADMIN_ALLOWED_HANDLES` 에 테스트용 ops 계정이 포함되어 있어야 함

| Endpoint | 기대 결과 | 결과 |
|---------|----------|------|
| `/actuator/health` | 200 / UP | PASS / FAIL |
| `/actuator/health/liveness` | 200 | PASS / FAIL |
| `/actuator/health/readiness` | 200 | PASS / FAIL |
| `/admin/health` | 미인증 401 / 비허용 ops 403 / 허용 ops 정상 응답 | PASS / FAIL |
| `/admin/health/realtime` | 허용 ops 계정에서 SSE 상태 표시 | PASS / FAIL |
| `/admin/health/reminder` | 허용 ops 계정에서 scheduler 상태 표시 | PASS / FAIL |

---

# 4. SSE 운영 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| SSE 연결 생성 | 채팅 진입 | PASS / FAIL |
| activeSseConnections 증가 | `/admin/health/realtime` 확인 | PASS / FAIL |
| 메시지 알림 실시간 수신 | 다른 계정에서 메시지 | PASS / FAIL |
| lastSseEventSentAt 갱신 | realtime health 확인 | PASS / FAIL |

---

# 5. Reminder Scheduler 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| Scheduler 실행 | 로그 확인 | PASS / FAIL |
| Reminder 알림 생성 | 리마인드 시간 도달 | PASS / FAIL |
| lastRunAt 갱신 | `/admin/health/reminder` | PASS / FAIL |
| lastSuccessAt 갱신 | `/admin/health/reminder` | PASS / FAIL |

---

# 6. 인증 / 세션 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| 로그인 성공 | 로그인 API | PASS / FAIL |
| refresh cookie 생성 | 브라우저 쿠키 확인 | PASS / FAIL |
| access 만료 후 refresh | 자동 갱신 | PASS / FAIL |
| 로그아웃 | 세션 제거 | PASS / FAIL |

---

# 7. 사용자 / 피드 기능 점검

| 기능 | 결과 |
|-----|------|
| 회원가입 | PASS / FAIL |
| handle 중복 확인 | PASS / FAIL |
| 프로필 조회(handle) | PASS / FAIL |
| 프로필 조회(userId) | PASS / FAIL |
| 글 작성 | PASS / FAIL |
| 댓글 작성 | PASS / FAIL |
| 댓글 reply | PASS / FAIL |
| 댓글 좋아요 | PASS / FAIL |
| 채팅 전송 | PASS / FAIL |
| Action 생성 | PASS / FAIL |
| 피드 공유 | PASS / FAIL |

---

# 8. Docker 상태

| 항목 | 확인 | 결과 |
|-----|------|------|
| mysql container | `docker ps` | PASS / FAIL |
| redis container | `docker ps` | PASS / FAIL |
| backend container | `docker ps` | PASS / FAIL |
| nginx container | `docker ps` | PASS / FAIL |
| container health | healthy 상태 | PASS / FAIL |

---

# 9. 로그 점검

| 항목 | 확인 | 결과 |
|-----|------|------|
| backend error log | `docker logs reallife-app` | PASS / FAIL |
| nginx error log | `docker logs reallife-nginx` | PASS / FAIL |
| scheduler log | reminder scheduler log | PASS / FAIL |

---

# 최종 판정

| 항목 | 결과 |
|-----|------|
| 빌드 / 테스트 | PASS / FAIL |
| REST Docs | PASS / FAIL |
| Health API | PASS / FAIL |
| SSE | PASS / FAIL |
| Reminder | PASS / FAIL |
| 핵심 기능 | PASS / FAIL |

---

## 최종 결과

**운영 가능 여부**

| 항목 | API | 확인 |
|-----|-----|-----|
| Admin Dashboard | /admin/dashboard | 허용 ops 계정에서 정상 |
| Admin Errors | /admin/errors | 허용 ops 계정에서 정상 |
| Alert Test | /admin/alerts/test | 허용 ops 계정에서 정상 |
| Alert History | /admin/alerts/history | 허용 ops 계정에서 정상 |
