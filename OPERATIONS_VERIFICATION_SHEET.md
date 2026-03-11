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
| admin-dashboard-get | generated-snippets/admin-dashboard-get | PASS / FAIL |
| admin-errors-get | generated-snippets/admin-errors-get | PASS / FAIL |
| admin-alert-test-post | generated-snippets/admin-alert-test-post | PASS / FAIL |
| admin-alert-history-get | generated-snippets/admin-alert-history-get | PASS / FAIL |
| users-exists | generated-snippets/users-exists | PASS / FAIL |
| users-profile-get-by-id | generated-snippets/users-profile-get-by-id | PASS / FAIL |

---

# 3. Health Endpoint 점검

| Endpoint | 기대 결과 | 결과 |
|---------|----------|------|
| `/actuator/health` | 200 / UP | PASS / FAIL |
| `/actuator/health/liveness` | 200 | PASS / FAIL |
| `/actuator/health/readiness` | 200 | PASS / FAIL |
| `/admin/health` | 인증 필요 / 정상 응답 | PASS / FAIL |
| `/admin/health/realtime` | SSE 상태 표시 | PASS / FAIL |
| `/admin/health/reminder` | scheduler 상태 표시 | PASS / FAIL |
| `/admin/dashboard` | 운영 요약 응답 | PASS / FAIL |
| `/admin/errors` | 최근 서버 에러 응답 | PASS / FAIL |
| `/admin/alerts/history` | 운영 알림 이력 응답 | PASS / FAIL |

---

# 4. SSE 운영 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| SSE 연결 생성 | 채팅 진입 | PASS / FAIL |
| activeSseConnections 증가 | `/admin/health/realtime` 확인 | PASS / FAIL |
| 메시지 알림 실시간 수신 | 다른 계정에서 메시지 | PASS / FAIL |
| lastSseEventSentAt 갱신 | realtime health 확인 | PASS / FAIL |
| 대시보드 realtime 카드 정상 표시 | `/ops/dashboard` 확인 | PASS / FAIL |

---

# 5. Reminder Scheduler 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| Scheduler 실행 | 로그 확인 | PASS / FAIL |
| Reminder 알림 생성 | 리마인드 시간 도달 | PASS / FAIL |
| lastRunAt 갱신 | `/admin/health/reminder` | PASS / FAIL |
| lastSuccessAt 갱신 | `/admin/health/reminder` | PASS / FAIL |
| 대시보드 reminder 카드 정상 표시 | `/ops/dashboard` 확인 | PASS / FAIL |

---

# 6. 운영 알림 / Slack 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| Slack 테스트 전송 성공 | `POST /admin/alerts/test` 또는 대시보드 버튼 | PASS / FAIL |
| Slack 채널 수신 확인 | 운영 채널 확인 | PASS / FAIL |
| 최근 운영 알림 이력 저장 | `/admin/alerts/history` 확인 | PASS / FAIL |
| FAILED alert pinned 노출 | `/ops/dashboard` 확인 | PASS / FAIL |
| alert history 필터 동작 | `/ops/dashboard` 확인 | PASS / FAIL |
| 최근 테스트 결과 카드 반영 | `/ops/dashboard` 확인 | PASS / FAIL |

---

# 7. 인증 / 세션 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| 로그인 성공 | 로그인 API | PASS / FAIL |
| refresh cookie 생성 | 브라우저 쿠키 확인 | PASS / FAIL |
| access 만료 후 refresh | 자동 갱신 | PASS / FAIL |
| 로그아웃 | 세션 제거 | PASS / FAIL |

---

# 8. 프론트 운영자 진입 점검

| 항목 | 확인 방법 | 결과 |
|-----|----------|------|
| `.env.local` allowlist 설정 | `VITE_OPS_ALLOWED_EMAILS`, `VITE_OPS_ALLOWED_HANDLES` 확인 | PASS / FAIL |
| 운영자 계정 노출 | `/me` 운영 도구 카드 확인 | PASS / FAIL |
| 비운영자 차단 | `/me?denied=ops` 리다이렉트 확인 | PASS / FAIL |
| 설정 안내 문구 표시 | `MeView.vue` 안내 영역 확인 | PASS / FAIL |
| `/ops/dashboard` 라우트 접근 | 운영자 계정으로 진입 확인 | PASS / FAIL |

---

# 9. 사용자 / 피드 기능 점검

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

# 10. Docker 상태

| 항목 | 확인 | 결과 |
|-----|------|------|
| mysql container | `docker ps` | PASS / FAIL |
| redis container | `docker ps` | PASS / FAIL |
| backend container | `docker ps` | PASS / FAIL |
| nginx container | `docker ps` | PASS / FAIL |
| container health | healthy 상태 | PASS / FAIL |

---

# 11. 로그 점검

| 항목 | 확인 | 결과 |
|-----|------|------|
| backend error log | `docker logs reallife-app` | PASS / FAIL |
| nginx error log | `docker logs reallife-nginx` | PASS / FAIL |
| scheduler log | reminder scheduler log | PASS / FAIL |
| ops alert log | Slack / cooldown / persist log 확인 | PASS / FAIL |

---

# 최종 판정

| 항목 | 결과 |
|-----|------|
| 빌드 / 테스트 | PASS / FAIL |
| REST Docs | PASS / FAIL |
| Health API | PASS / FAIL |
| SSE | PASS / FAIL |
| Reminder | PASS / FAIL |
| 운영 알림 / Slack | PASS / FAIL |
| 운영자 진입 UX | PASS / FAIL |
| 핵심 기능 | PASS / FAIL |

---

## 최종 결과

**운영 가능 여부**

- [ ] 운영 가능
- [ ] 조건부 운영 가능
- [ ] 배포 보류

### 메모
- 장애 원인:
- 후속 조치:
- 재검증 예정일:
