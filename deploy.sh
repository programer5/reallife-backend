#!/usr/bin/env bash
set -euo pipefail

BRANCH="${1:-main}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "== Deploy branch: $BRANCH =="
echo "== Project dir: $PROJECT_DIR =="

git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"

if [ ! -f .env ]; then
  echo "ERROR: .env not found."
  echo "Create it first: cp .env.example .env && nano .env"
  exit 1
fi

COMMIT_SHA="$(git rev-parse --short HEAD)"
BUILD_TIME="$(date -Iseconds)"

upsert_env () {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" .env; then
    if sed --version >/dev/null 2>&1; then
      sed -i "s|^${key}=.*|${key}=${value}|g" .env
    else
      sed -i '' "s|^${key}=.*|${key}=${value}|g" .env
    fi
  else
    echo "${key}=${value}" >> .env
  fi
}

upsert_env "APP_VERSION" "$COMMIT_SHA"
upsert_env "APP_BUILD_TIME" "$BUILD_TIME"

echo "== Version updated in .env =="
echo "APP_VERSION=$COMMIT_SHA"
echo "APP_BUILD_TIME=$BUILD_TIME"

echo "== Docker down =="
docker compose down

echo "== Docker build (app + frontend-build) =="
docker compose build --no-cache app frontend-build

echo "== Docker up =="
docker compose up -d

echo "== Restart nginx =="
docker compose restart nginx

echo "== Containers =="
docker compose ps

echo "== Health check =="
for i in {1..30}; do
  if curl -fsS "http://localhost/api/health" >/dev/null; then
    echo "OK: /api/health"
    break
  fi
  echo "waiting... ($i/30)"
  sleep 2
done

echo "== Version check =="
curl -fsS "http://localhost/api/version" || true
echo
echo "== Deploy done =="