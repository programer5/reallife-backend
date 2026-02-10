# DB Runbook (MySQL) — Comments Cursor Index & Cleanup

이 문서는 댓글 커서 페이징 성능을 위한 인덱스 추가/정리 작업을
운영 환경에서도 안전하게 적용하기 위한 Runbook 입니다.

---

## Background

댓글 목록 조회는 아래 정렬/커서 조건을 사용합니다.

- 정렬: `created_at DESC, id DESC`
- 커서 조건:
    - `created_at < cursorCreatedAt`
    - 또는 `created_at = cursorCreatedAt AND id < cursorId`

따라서 최적 인덱스는:

✅ `idx_comments_post_created_id (post_id, created_at, id)`

기존에 아래 인덱스가 이미 있을 수 있습니다.

- `idx_comment_post_created (post_id, created_at)`  ← 중복/겹침 가능

두 인덱스가 함께 있으면 쓰기 비용/저장 공간이 늘 수 있으므로,
신규 인덱스 유지 + 기존 인덱스 제거를 권장합니다.

---

## Files

- `2026-02-10_add_comments_cursor_index_mysql.sql`
    - 인덱스 추가 (없으면 생성)
- `2026-02-10_drop_comment_duplicate_index_mysql.sql`
    - 중복/겹치는 인덱스 조건부 제거(있으면 drop, 없으면 no-op)

---

## Preconditions (필수 확인)

```sql
SELECT DATABASE() AS current_db;
SELECT VERSION() AS mysql_version;
SHOW TABLES LIKE 'comments';
SHOW TABLES LIKE 'flyway_schema_history';