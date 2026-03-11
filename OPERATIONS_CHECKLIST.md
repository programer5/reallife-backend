# OPERATIONS_CHECKLIST.md

RealLife 서비스 운영 안정화를 위한 **배포 전 / 운영 중 체크 문서**

이 문서는 다음 목적을 가진다.

-   배포 전 필수 체크
-   기능 회귀 테스트
-   SSE / 인증 / 쿠키 안정성 확인
-   문서 및 테스트 정합성 유지
-   장애 발생 시 확인 위치 안내

------------------------------------------------------------------------

# 1. 배포 전 빌드 확인

## Backend build

``` bash
./gradlew clean build
```

확인 사항

-   build 실패 없음
-   compile warning 과도하지 않음
-   jar 정상 생성

```{=html}
<!-- -->
```
    build/libs/backend-*.jar

------------------------------------------------------------------------

## REST Docs 생성

``` bash
./gradlew clean test asciidoctor
```

확인 사항

    build/docs/asciidoc/index.html

-   문서 정상 생성
-   include snippet 깨짐 없음
-   controller 변경 후 docs 동기화 확인

------------------------------------------------------------------------

# 2. 기능 회귀 테스트

운영 안정화를 위해 아래 기능은 **항상 배포 전 확인한다.**

## 인증

확인 항목

-   회원가입 정상 동작
-   로그인 정상 동작
-   refresh cookie 발급
-   refresh cookie 재발급

체크

-   로그인 후 새로고침 → 세션 유지
-   쿠키 삭제 → 인증 해제

------------------------------------------------------------------------

## 피드

확인 항목

-   글 작성
-   글 수정
-   글 삭제
-   피드 목록 조회

체크

-   최신 글 정상 노출
-   작성 후 피드 즉시 반영

------------------------------------------------------------------------

## 댓글

확인 항목

-   댓글 작성
-   댓글 reply
-   댓글 좋아요
-   댓글 목록

체크

-   댓글 수 증가
-   좋아요 수 증가
-   새로고침 후 정합성 유지

------------------------------------------------------------------------

## Action 흐름

핵심 기능

    댓글
    ↓
    Action 생성
    ↓
    Conversation Dock
    ↓
    Action Timeline
    ↓
    Reminder

체크

-   댓글에서 Action 생성
-   Action Dock 표시
-   Conversation에서 Action 유지
-   Reminder 생성

------------------------------------------------------------------------

## 메시지

확인 항목

-   채팅 생성
-   메시지 전송
-   메시지 읽음 처리
-   unread 표시

체크

-   메시지 전송 후 즉시 표시
-   unread count 정상 감소

------------------------------------------------------------------------

# 3. SSE 안정성 체크

RealLife는 다음 이벤트를 SSE로 전달한다.

    MESSAGE_RECEIVED
    COMMENT_CREATED
    POST_LIKED
    PIN_CREATED
    PIN_REMIND

확인 사항

-   새 메시지 SSE 수신
-   댓글 생성 SSE 수신
-   리마인더 SSE 수신
-   중복 이벤트 없음

------------------------------------------------------------------------

# 4. Cookie / 인증 운영 체크

확인 항목

-   refresh cookie 정상 발급
-   SameSite 설정
-   Secure 설정
-   domain/path 확인

체크

Application → Cookies

확인

-   refresh cookie 존재
-   만료시간 정상
-   로그인 유지

------------------------------------------------------------------------

# 5. Flyway Migration 체크

확인

    db/migration

체크

-   migration 누락 없음
-   checksum mismatch 없음

문제 발생 시

    flyway repair

------------------------------------------------------------------------

# 6. 업로드 파일 관리

확인 항목

    uploads/

체크

-   이미지 업로드 정상
-   접근 URL 정상

------------------------------------------------------------------------

# 7. Docker 상태 확인

컨테이너 상태

    docker ps

예시

    reallife-nginx
    reallife-app
    reallife-redis
    reallife-mysql

확인

-   Restarting 상태 없음
-   Health check 정상

------------------------------------------------------------------------

# 8. 배포 후 체크

배포 후 아래 기능을 빠르게 확인한다.

-   로그인
-   피드 조회
-   글 작성
-   댓글 작성
-   메시지 전송
-   SSE 이벤트 수신
-   Reminder 알림

------------------------------------------------------------------------

# 9. 로그 확인

Backend

    docker logs reallife-app

nginx

    /var/log/nginx/error.log


## 운영 대시보드 점검

1. `/admin/dashboard` 호출 정상 여부 확인
2. `/admin/errors` 최근 서버 에러 조회 확인
3. `/admin/alerts/test` Slack 테스트 알림 전송
4. `/admin/alerts/history` 운영 알림 이력 확인

## 프론트 운영 확인

1. `/me` 화면에서 운영 도구 카드 노출 확인
2. `/ops/dashboard` 진입 확인
3. Health / Realtime / Reminder 상태 확인
