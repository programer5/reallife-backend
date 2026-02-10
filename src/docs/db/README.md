## 인덱스 정리 (Comments)

### 배경
- 기존 인덱스: `idx_comment_post_created` (`post_id`, `created_at`)
- 추가된 인덱스: `idx_comments_post_created_id` (`post_id`, `created_at`, `id`)

EXPLAIN 분석 결과, 두 인덱스는 일반적인 댓글 목록 조회 쿼리에 대해
동일한 실행 계획을 사용함:
- `Using where; Backward index scan`
- 조회 rows, Extra 항목에서 유의미한 차이 없음

두 인덱스가 기능적으로 크게 겹치는 상태로 동시에 유지될 경우,
다음과 같은 비용이 증가할 수 있음:
- 쓰기 비용 증가 (INSERT / UPDATE / DELETE)
- 디스크 저장 공간 사용 증가

---

### 권장 사항
커서 페이징에서 사용하는 키 구조에 더 적합한 신규 인덱스만 유지하고,
기존 인덱스는 제거하는 것을 권장함.

- ✅ 유지: `idx_comments_post_created_id` (`post_id`, `created_at`, `id`)
- ❌ 제거: `idx_comment_post_created` (`post_id`, `created_at`)

---

### 적용 방법 (MySQL)
운영 트래픽이 낮은 시간대에 실행하는 것을 권장함.

```sql
DROP INDEX idx_comment_post_created ON comments;