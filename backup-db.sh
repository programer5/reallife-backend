#!/usr/bin/env bash
set -euo pipefail

# 실행 위치: backend 폴더
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Create it first:"
  echo "  cp .env.example .env && nano .env"
  exit 1
fi

# .env 로드(간단 로더)
set -a
source .env
set +a

TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${BACKUP_DIR:-./backups}"
FILE="${OUT_DIR}/mysql_${MYSQL_DATABASE}_${TS}.sql.gz"

mkdir -p "$OUT_DIR"

echo "== Backup start =="
echo "DB: ${MYSQL_DATABASE}"
echo "Output: ${FILE}"

# mysqldump는 mysql 컨테이너 안에서 실행(호스트에 mysql 클라이언트 없어도 됨)
docker exec -i reallife-mysql \
  mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction --routines --triggers \
  "${MYSQL_DATABASE}" \
  | gzip > "${FILE}"

echo "== Backup done =="
ls -lh "${FILE}"