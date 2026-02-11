# DB Runbook (MySQL + Flyway)

이 프로젝트는 DB 변경(테이블/인덱스)을 Flyway Migration으로 관리합니다.

Flyway migration 경로:
src/main/resources/db/migration

운영 적용 방식:
애플리케이션 시작 시 자동 migrate

--------------------------------------------

## 1. 현재 적용 상태 확인

MySQL 콘솔에서 실행:

USE backend;

SELECT installed_rank, version, description, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

정상 예시:
1  << Flyway Baseline >>
2  V2__add_comments_cursor_index.sql
3  V3__drop_comment_duplicate_index.sql
success = 1

--------------------------------------------

## 2. Comments Cursor Pagination 인덱스 전략

댓글 목록은 Cursor 기반 페이징을 사용합니다.

정렬 기준:
ORDER BY created_at DESC, id DESC

커서 조건:
created_at < ?
OR (created_at = ? AND id < ?)

최적 인덱스:
idx_comments_post_created_id (post_id, created_at, id)

제거 대상(중복 인덱스):
idx_comment_post_created (post_id, created_at)

--------------------------------------------

## 3. 운영 적용 방법

운영 환경 설정:

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

앱을 재시작하면 Flyway가 자동으로 migration을 적용합니다.

--------------------------------------------

## 4. 적용 후 검증

### 1) 인덱스 확인

SHOW INDEX FROM comments;

남아 있어야 하는 인덱스:
idx_comments_post_created_id

### 2) 실행 계획 확인

EXPLAIN
SELECT id, author_id, content, created_at
FROM comments
WHERE post_id = '<POST_UUID>'
AND deleted = false
ORDER BY created_at DESC, id DESC
LIMIT 11;

정상:
key 컬럼이 idx_comments_post_created_id 로 선택됨
Extra: Using where; Backward index scan

--------------------------------------------

## 5. 롤백 (필요한 경우)

CREATE INDEX idx_comment_post_created
ON comments (post_id, created_at);

일반적으로는 (post_id, created_at, id) 인덱스 하나로 충분합니다.