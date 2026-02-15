# System Diagram (High-level)

## Components

- Client (Web / Mobile)
- Nginx (Reverse Proxy)
- Spring Boot App (scale-out: N instances)
- Redis (Pub/Sub)
- MySQL (Source of Truth)
- REST Docs (/docs)

---

## High-level Architecture

Client
├─ REST API 요청 ────────────────> Nginx ───────────────> Spring Boot (REST)
└─ SSE Subscribe (EventSource) ──> Nginx ───────────────> Spring Boot (SSE)

Spring Boot (REST)
├─ DB write/read ────────────────> MySQL
└─ publish SSE event ────────────> Redis Pub/Sub (channel: sse:push)

Redis Pub/Sub
└─ fan-out (to all instances) ───> Spring Boot (Subscriber)

Spring Boot (Subscriber)
└─ emit event ───────────────────> Client SSE connection

---

## Real-time Flow (Message / Notification)

1) REST API로 메시지 전송 / 알림 생성 트리거
2) 트랜잭션 커밋 성공 후(AFTER_COMMIT) 이벤트 발행
3) Redis Pub/Sub 채널(sse:push)에 publish
4) 모든 인스턴스의 subscriber가 수신
5) 각 인스턴스는 자신의 메모리 emitter들에게 push
6) 클라이언트는 event name으로 분기 처리
    - message-created
    - notification-created
    - ping (heartbeat)

---

## Reliability Notes

### Why Redis Pub/Sub?
- SSE connection(emitter)은 메모리 기반이라, scale-out 시 다른 인스턴스에 연결된 사용자에게 이벤트가 전달되지 않을 수 있음
- Pub/Sub를 통해 “모든 인스턴스가 동일 이벤트를 수신”하고,
  각 인스턴스가 자기 emitter에게 push → 멀티 인스턴스에서 안정적

### Why AFTER_COMMIT?
- DB write 실패(rollback)인데 SSE/알림만 나가면 정합성 깨짐
- commit 성공 후에만 외부로 이벤트를 내보내 정합성 확보

### Last-Event-ID (Next Step)
- 모바일/불안정 네트워크에서 SSE 끊김이 자주 발생
- 끊겼다가 재연결 시 Last-Event-ID 이후 이벤트를 재전송하면 유실 방지 가능