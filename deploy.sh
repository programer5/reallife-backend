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

wait_for_health () {
  local container_name="$1"
  local max_attempts="$2"
  local sleep_seconds="$3"

  echo "== Waiting for ${container_name} to become healthy =="
  for ((i=1; i<=max_attempts; i++)); do
    local status
    status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$container_name" 2>/dev/null || true)"

    if [ "$status" = "healthy" ]; then
      echo "OK: ${container_name} is healthy"
      return 0
    fi

    if [ "$status" = "unhealthy" ]; then
      echo "ERROR: ${container_name} is unhealthy"
      docker compose logs "$container_name" --tail=200 || true
      return 1
    fi

    echo "waiting ${container_name}... (${i}/${max_attempts}) status=${status:-missing}"
    sleep "$sleep_seconds"
  done

  echo "ERROR: timed out waiting for ${container_name}"
  docker compose logs "$container_name" --tail=200 || true
  return 1
}

upsert_env "APP_VERSION" "$COMMIT_SHA"
upsert_env "APP_BUILD_TIME" "$BUILD_TIME"
upsert_env "SPRING_PROFILES_ACTIVE" "docker"

echo "== Version updated in .env =="
echo "APP_VERSION=$COMMIT_SHA"
echo "APP_BUILD_TIME=$BUILD_TIME"
echo "SPRING_PROFILES_ACTIVE=docker"

echo "== Docker down =="
docker compose down

echo "== Docker build (app + frontend-build) =="
docker compose build --no-cache app frontend-build

echo "== Start infra first =="
docker compose up -d reallife-mysql redis elasticsearch

wait_for_health reallife-mysql 60 2
wait_for_health reallife-redis 40 2

echo "== Start app/front/nginx =="
docker compose up -d app frontend-build nginx

wait_for_health reallife-app 60 2
wait_for_health reallife-nginx 40 2

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
