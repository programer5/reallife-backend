## Index cleanup (comments)

### Background
- Existing index: `idx_comment_post_created` (post_id, created_at)
- Added index: `idx_comments_post_created_id` (post_id, created_at, id)

EXPLAIN showed both indexes produce the same plan for typical queries:
- `Using where; Backward index scan`
- No meaningful difference in rows/extra

When two indexes overlap heavily, keeping both can increase:
- write cost (INSERT/UPDATE/DELETE)
- storage usage

### Recommendation
Keep the newer index that matches the cursor key:
- ✅ Keep: `idx_comments_post_created_id` (post_id, created_at, id)
- ❌ Drop: `idx_comment_post_created` (post_id, created_at)

### How to apply (MySQL)
Run during a low-traffic window.

```sql
DROP INDEX idx_comment_post_created ON comments;