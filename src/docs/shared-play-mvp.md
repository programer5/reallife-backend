# Shared Play MVP 설계 초안

## 목적

메시지 상세(ConversationDetail) 안에서 **같이 보기 / 같이 듣기 세션**을 만들고,
대화 → 공동 경험 → 액션/리마인더/공유 흐름으로 이어지게 만든다.

## 제품 흐름

1. 대화 상세에서 `같이 보기` 또는 `같이 듣기` 시작
2. 외부 링크(YouTube, Spotify, 일반 비디오 URL 등) 입력
3. 세션 카드가 메시지 흐름 안에 생성됨
4. 참여자가 동일 세션에 입장
5. MVP 범위에서는 `play / pause / seek`만 동기화
6. 세션 종료 후 액션 생성 또는 피드 공유로 이어짐

## MVP 범위

- 링크 기반 세션 생성
- conversation 안에 session card 표시
- play / pause / seek 동기화
- 현재 재생 위치 / 진행 상태 / 호스트 표시
- 종료 후 `액션 만들기`, `공유하기` CTA 제공

## 비범위(후속)

- 내장 스트리밍 호스팅
- 완전한 실시간 음성/영상 통화
- DRM/OTT 직접 재생
- 복수 플레이어 정교한 싱크 보정
- AI 추천/요약

## 프론트 우선 작업

### 1. ConversationDetail 구조 분리
- search return / highlight 로직 분리
- action dock 분리
- message thread 분리
- composer / attachment / capsule 분리
- 이후 session card가 들어갈 위치 확보

### 2. 새 UI 블록
- `ConversationSessionComposer.vue`
- `ConversationSessionCard.vue`
- `useConversationSessions.js`

### 3. UX 요구사항
- 세션 생성은 메시지 입력 근처에서 시작 가능
- 카드에서 상태가 바로 보이도록 구성
- 모바일에서는 bottom sheet 중심
- 종료 후 액션/공유 버튼 유지

## 백엔드 우선 작업

### 1. 도메인
- `PlaybackSession`
- `PlaybackSessionParticipant`
- `PlaybackSessionEvent`(선택)

### 2. API 초안
- `POST /api/conversations/{conversationId}/sessions`
- `GET /api/conversations/{conversationId}/sessions`
- `POST /api/conversations/{conversationId}/sessions/{sessionId}/state`
- `POST /api/conversations/{conversationId}/sessions/{sessionId}/end`

### 3. 이벤트/SSE
- `SESSION_CREATED`
- `SESSION_UPDATED`
- `SESSION_ENDED`

### 4. 메시지 연계
- `MessageType.SESSION` 활용
- message.metadataJson 또는 별도 DTO로 session summary 노출
- session 종료 후 action/share 생성 링크 제공

## 데이터 초안

### PlaybackSession
- id
- conversationId
- creatorId
- sessionType (`WATCH`, `LISTEN`)
- sourceType (`YOUTUBE`, `SPOTIFY`, `DIRECT_URL`, `OTHER`)
- sourceUrl
- title
- thumbnailUrl
- state (`CREATED`, `PLAYING`, `PAUSED`, `ENDED`)
- positionMs
- startedAt
- endedAt

### PlaybackSessionParticipant
- id
- sessionId
- userId
- role (`HOST`, `PARTICIPANT`)
- joinedAt
- leftAt

## 운영 원칙

- MVP는 링크/메타데이터 기반으로 시작
- 재생 권한/실제 콘텐츠 권한은 외부 플랫폼 정책을 존중
- 서버는 상태 동기화와 대화 흐름 연결에 집중
- 장애 시 세션 기능만 축소되고 메시지/검색/액션 핵심 흐름은 유지

## 런칭 전 우선순위

1. ConversationDetail 구조 정리
2. Search UX 마감
3. 검색/운영 체크리스트 보강
4. Shared Play MVP API/카드 스캐폴드
5. play/pause/seek 동기화
6. action/share 연결
