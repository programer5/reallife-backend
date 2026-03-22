# Elasticsearch quickstart

## 1. 인덱스 생성
```bash
curl -X PUT "http://localhost:9200/reallife_search" \
  -H "Content-Type: application/json" \
  --data-binary @src/main/resources/search/reallife-search-index-v1.json
```

## 2. 애플리케이션 설정
```yaml
app:
  search:
    elastic:
      enabled: true
      base-url: http://localhost:9200
      index-name: reallife_search
```

## 3. 메타 백엔드 확인
- ES 성공 조회면 `meta.backend = elasticsearch`
- ES 비활성 또는 장애 fallback이면 `meta.backend = db-fallback`
