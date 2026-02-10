# Architecture Decisions (ADR)

이 문서는 RealLife 프로젝트에서 “왜 이런 설계를 선택했는지”를 정리합니다.
(코드 리뷰/인수인계/면접/운영 관점에서 빠르게 맥락을 전달하기 위한 문서)

---

## 1) 왜 Cursor Pagination을 선택했는가?

### 문제 (Offset Pagination)
- 데이터가 커질수록 OFFSET이 커지고, skip cost가 증가함
- 중간 데이터 삽입/삭제 시 페이지가 흔들릴 수 있음(중복/누락)

### 선택 (Cursor Pagination)
- `createdAt + id`를 기준으로 커서를 만들고 LIMIT 기반으로 다음 페이지 조회
- 정렬: `ORDER BY created_at DESC, id DESC`
- 다음 페이지 조건 예시:
    - `created_at < :cursorCreatedAt`
    - OR (`created_at = :cursorCreatedAt AND id < :cursorId`)

### 결과
- 대용량에서도 안정적인 성능
- SNS 피드/댓글/메시지 같은 “최신순 목록”에 적합

---

## 2) UUID를 PK로 사용한 이유

### 목표
- URL/리소스 ID 추측 공격 방지
- 내부 리소스 식별자를 외부에 안전하게 노출

### 선택
- 모든 엔티티 PK를 UUID로 통일
- 컨트롤러는 UUID만 다룸

### 고려사항
- 인덱스/스토리지 비용 증가 가능
- 목록 조회는 커서 페이징 + 복합 인덱스로 최적화하여 보완

---

## 3) Comment ↔ Post 연관관계를 제거한 이유

### 문제
- JPA 연관관계는 편하지만, 도메인 결합도와 조회/삭제 비용이 커질 수 있음
- 불필요한 JOIN 증가 및 삭제 전파/연쇄 로직 복잡도 증가 가능

### 선택
- Comment는 Post를 참조하지 않고 `postId(UUID)`만 보유
- 목적: 결합도 감소, 조회/삭제 성능 예측 가능성, 보안(리소스 추측 최소화)

### 결과
- Comment 도메인이 독립적으로 동작
- 향후 MSA 분리 시 수정 비용 최소화

---

## 4) REST Docs를 선택한 이유

### 목표
- 문서가 실제 동작과 반드시 일치하도록 보장

### 선택
- Spring REST Docs 기반(MockMvc) 테스트 성공 시 문서 생성

### 결과
- “테스트 통과 = 문서 신뢰성” 확보
- CI에서 자동 생성/배포까지 연결 가능

---

## 5) 이벤트 기반 Notification 설계

### 문제
- 메시지 전송 로직과 알림 생성 로직이 강하게 결합되면 유지보수가 어려움

### 선택
- 도메인 이벤트 기반으로 분리
- `@TransactionalEventListener(AFTER_COMMIT)` 사용
    - 트랜잭션이 성공한 뒤에만 알림 생성

### 결과
- 데이터 정합성 확보(커밋 성공 후 처리)
- 추후 Kafka 등 메시지 브로커로 확장 가능한 구조