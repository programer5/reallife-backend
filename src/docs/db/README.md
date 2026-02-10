# DB Runbook (MySQL)

## 2026-02-10 - Add comments cursor pagination index

### Why
- Comments list uses cursor pagination ordered by `created_at DESC, id DESC`
- Filters by `post_id` (and usually `deleted=false`)
- Composite index improves performance as comments grow

### What
- Add index: `idx_comments_post_created_id` on `(post_id, created_at, id)`

### Script
- `src/docs/db/2026-02-10_add_comments_cursor_index_mysql.sql`

### How to apply (MySQL)

#### Option A) mysql CLI로 실행 (추천)
```bash
mysql -h <host> -u <user> -p <db_name> < src/docs/db/2026-02-10_add_comments_cursor_index_mysql.sql

### Note (Index duplication)
- Existing index: idx_comment_post_created (post_id, created_at)
- New index: idx_comments_post_created_id (post_id, created_at, id)
If both exist and query plans show no benefit, consider dropping the older one to reduce write cost:
```sql
DROP INDEX idx_comment_post_created ON comments;