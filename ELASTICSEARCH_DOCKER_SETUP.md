# RealLife Elasticsearch Docker 적용 가이드

## 왜 빨간줄이 났는가
이전 파일은 YAML 들여쓰기가 깨져 있었습니다.
특히 `elasticsearch`, `kibana`, `app` 블록의 들여쓰기가 `services:` 아래로 맞지 않아 편집기에서 빨간줄이 보일 수 있습니다.

이번 파일은 현재 프로젝트의 실제 `.env` 변수명과 Docker 구조에 맞춰 다시 정리한 버전입니다.

## 1. 교체할 파일
- `docker-compose.yml`
- `src/main/resources/application-docker.yml`

## 2. .env에 추가할 값
기존 `.env`는 유지하고, 아래 값만 없으면 맨 아래에 추가하세요.

```env
ELASTIC_PORT=9200
KIBANA_PORT=5601
ELASTIC_ES_JAVA_OPTS=-Xms512m -Xmx512m
SEARCH_ELASTIC_ENABLED=true
SEARCH_ELASTIC_BASE_URL=http://elasticsearch:9200
SEARCH_ELASTIC_INDEX_NAME=reallife_search
SEARCH_ELASTIC_API_KEY=
```

## 3. Windows에서 vm.max_map_count 설정
Git Bash에서 `sudo`가 안 되는 건 정상입니다.
PowerShell 또는 CMD에서 아래처럼 실행하세요.

```powershell
wsl -d docker-desktop -u root
sysctl -w vm.max_map_count=1048576
exit
```

만약 `docker-desktop` 배포판이 없다고 나오면 먼저 아래로 목록을 확인하세요.

```powershell
wsl -l -v
```

## 4. 컨테이너 실행
백엔드 프로젝트 루트에서 실행:

```bash
docker compose down
docker compose up -d --build
docker compose ps
```

## 5. Elasticsearch 확인
```bash
curl http://localhost:9200
```

정상 응답이면 JSON이 나옵니다.

## 6. 인덱스 생성
PowerShell/CMD:

```bash
curl -X PUT "http://localhost:9200/reallife_search" -H "Content-Type: application/json" --data-binary "@src/main/resources/search/reallife-search-index-v1.json"
```

Git Bash / WSL:

```bash
curl -X PUT "http://localhost:9200/reallife_search" \
  -H "Content-Type: application/json" \
  --data-binary @src/main/resources/search/reallife-search-index-v1.json
```

## 7. 검색이 ES를 타는지 확인
앱 실행 후:

```bash
curl "http://localhost:8080/api/search?q=test&type=ALL&limit=6"
```

응답의 `meta.backend`가:
- `elasticsearch` 이면 ES 조회 성공
- `db-fallback` 이면 DB fallback

## 8. 아주 중요한 포인트
현재 구조는 새로 생성/수정되는 메시지, 액션, 캡슐, 게시글부터 ES에 반영되는 흐름이 중심입니다.
기존 DB 전체 데이터를 한 번에 넣는 reindex/backfill은 별도 작업이 필요할 수 있습니다.
