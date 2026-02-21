#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

if [ $# -lt 1 ]; then
  echo "Usage: ./restore-db.sh <backup.sql.gz>"
  exit 1
fi

BACKUP_FILE="$1"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Create it first:"
  echo "  cp .env.example .env && nano .env"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: backup file not found: $BACKUP_FILE"
  exit 1
fi

set -a
source .env
set +a

echo "== Restore start =="
echo "DB: ${MYSQL_DATABASE}"
echo "File: ${BACKUP_FILE}"
echo "WARNING: This will overwrite existing data!"

gunzip -c "$BACKUP_FILE" | docker exec -i reallife-mysql \
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}"

echo "== Restore done =="