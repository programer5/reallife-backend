# Search deployment checklist

## 1. Elasticsearch up 확인
```bash
curl http://localhost:9200
```

정상 JSON 응답이 내려오면 Elasticsearch 컨테이너가 정상 기동된 상태입니다.

## 2. 인덱스 존재 확인
```bash
curl "http://localhost:9200/_cat/indices?v"
```

`reallife_search` 인덱스가 목록에 보여야 합니다.

## 3. 검색 백엔드 확인
```bash
curl "http://localhost/api/search?q=test&type=ALL&limit=5"
```

응답의 `meta.backend` 값을 확인합니다.

- `elasticsearch`: ES 조회 성공
- `db-fallback`: ES 비활성 또는 장애 fallback

## 4. 재색인 성공 확인
관리자 인증과 `X-Search-Reindex-Token` 헤더를 함께 사용합니다.

```bash
curl -X POST "http://localhost/api/search/admin/reindex?batchSize=300" \
  -H "Authorization: Bearer <accessToken>" \
  -H "X-Search-Reindex-Token: <reindexToken>"
```

응답에서 아래를 확인합니다.

- `backend = elasticsearch`
- `totals.indexed > 0`
- `messages.indexed`, `actions.indexed`, `capsules.indexed`, `posts.indexed`

## 5. ES 문서 수 확인
```bash
curl "http://localhost:9200/reallife_search/_count?pretty"
```

문서 수가 0보다 크면 실제 인덱싱이 들어간 상태입니다.

## 6. 운영 로그 확인 포인트
애플리케이션 로그에서 아래 패턴을 확인합니다.

- `search backend=elasticsearch ...`
- `search backend=elasticsearch failed. fallback=db ...`
- `search backend=db-fallback ...`
- `search reindex started ...`
- `search reindex progress ...`
- `search reindex finished ...`
