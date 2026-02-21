#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${BACKUP_DIR:-./backups}"
SRC_DIR="./uploads"
FILE="${OUT_DIR}/uploads_${TS}.tar.gz"

mkdir -p "$OUT_DIR"

if [ ! -d "$SRC_DIR" ]; then
  echo "ERROR: uploads directory not found: $SRC_DIR"
  exit 1
fi

echo "== Uploads backup start =="
echo "Source: ${SRC_DIR}"
echo "Output: ${FILE}"

tar -czf "${FILE}" -C "${SRC_DIR}" .

echo "== Uploads backup done =="
ls -lh "${FILE}"