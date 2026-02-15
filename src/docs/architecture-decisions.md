# Architecture Decisions (ADR)

이 문서는 RealLife 프로젝트에서 “왜 이런 설계를 선택했는지”를 정리합니다.  
(코드 리뷰 / 인수인계 / 면접 / 운영 관점에서 빠르게 맥락 전달 목적)

---

## 1) 왜 Cursor Pagination을 선택했는가?

### 문제 (Offset Pagination)
- 데이터가 커질수록 OFFSET 증가 → skip cost 증가
- 중간 데이터 삽입/삭제 시 페이지 흔들림(중복/누락)

### 선택
createdAt + id 기반 커서

정렬
ORDER BY created_at DESC, id DESC

다음 페이지 조건
created_at < :cursorCreatedAt
OR (created_at = :cursorCreatedAt AND id < :cursorId)

### 결과
- 대용량에서도 일정한 성능
- SNS 피드/댓글/메시지에 적합

---

## 2) UUID를 PK로 사용한 이유

### 목표
- 리소스 ID 추측 공격 방지
- 외부 노출 안전성

### 선택
- 모든 엔티티 PK = UUID

### Trade-off
- 인덱스 비용 증가 → 복합 인덱스 + 커서 페이징으로 해결

---

## 3) Comment ↔ Post 연관관계를 제거한 이유

### 문제
JPA 연관관계 사용 시
- 불필요한 JOIN 증가
- 삭제 전파 로직 복잡
- 도메인 결합도 증가

### 선택
Comment는 postId(UUID)만 저장 (연관관계 제거)

### 결과
- 도메인 독립성 증가
- 조회/삭제 비용 예측 가능
- 향후 서비스 분리 용이

---

## 4) REST Docs를 선택한 이유

### 목표
문서 신뢰성 = 테스트 신뢰성

### 선택
Spring REST Docs (MockMvc 기반 테스트 성공 시 문서 생성)

### 결과
- 문서와 실제 동작 불일치 방지
- CI 자동 문서 배포 가능

---

## 5) 이벤트 기반 Notification 설계

### 문제
메시지 로직 내부에서 알림 생성 시 결합도 증가

### 선택
@TransactionalEventListener(AFTER_COMMIT)

### 이유
- 트랜잭션 성공 후에만 알림 생성
- 롤백 시 잘못된 알림 방지

### 결과
정합성 + 확장성 확보 (Kafka 확장 가능 구조)

---

## 6) 왜 SSE(Server-Sent Events)를 선택했는가?

### 대안 비교
Polling: 불필요한 요청 과다  
Long Polling: 서버 자원 점유  
WebSocket: 상태 관리 복잡 / 인증 / 인프라 비용  
SSE: HTTP 기반 + 단방향 알림에 적합

### 선택 이유
- 알림/DM은 서버 → 클라이언트 단방향 이벤트
- 모바일/브라우저 호환성 우수
- 로드밸런서 친화적
- 구현 복잡도 낮음

### 결과
가벼운 실시간 알림 시스템 구현

---

## 7) Redis Pub/Sub을 사용한 이유 (SSE 확장성)

### 문제
SSE emitter는 메모리에 존재 → 멀티 인스턴스 환경에서 이벤트 손실

### 해결 구조
Publisher(API 서버) → Redis Pub/Sub → 모든 인스턴스 Subscriber → 각자의 SSE emitter 전송

### 결과
- 어떤 서버에 연결되어도 이벤트 수신
- 수평 확장 가능 구조

---

## 8) Last-Event-ID 재전송을 지원한 이유

### 문제
모바일 환경에서 네트워크 끊김 빈번 → 이벤트 유실 발생

### 해결
- 클라이언트가 마지막 이벤트 ID 전송
- 서버가 이후 이벤트 재전송

### 효과
- “알림 안 왔어요” 문제 해결
- 모바일 친화적 실시간 시스템

---

## 9) Flyway + ddl-auto=validate 전략

### 문제
JPA auto update 사용 시
- 운영 DB 드리프트 발생
- 롤백 불가
- 장애 원인 추적 어려움

### 선택
- 모든 스키마 변경 = Migration Script
- 애플리케이션 시작 시 validate

### 결과
- 스키마 변경 이력 추적 가능
- 운영 안정성 확보

---

## 10) AFTER_COMMIT 이벤트를 사용한 이유

### 문제
트랜잭션 내에서 외부 동작 실행 시 롤백 불일치 발생

예)
메시지 저장 실패 → 알림은 전송됨

### 해결
AFTER_COMMIT 이벤트 발행

### 결과
DB 상태와 외부 상태 일치

---

## 결론

이 프로젝트의 핵심 설계 목표는 다음 세 가지입니다:

1. 데이터 정합성 (AFTER_COMMIT, Flyway validate)
2. 실시간 안정성 (SSE + Redis + Last-Event-ID)
3. 대용량 성능 (Cursor Pagination)

단순 CRUD가 아닌,
“운영 가능한 SNS 백엔드 구조”를 목표로 설계되었습니다.

- 로그인 Rate Limit은 Redis(INCR+EXPIRE) 기반으로 구현하여 멀티 인스턴스 환경에서도 동일하게 동작하도록 했습니다. 또한 email+IP 복합키로 제한하여 공격/오탐 모두를 줄였습니다.
- Refresh Token은 로테이션을 적용했고, revoke된 refresh 재사용(reuse) 시 전체 로그아웃으로 탈취 대응을 강화했습니다.