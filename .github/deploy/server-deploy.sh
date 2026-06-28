#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/root/think}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.server-test.yml}"

cd "$APP_DIR"

COMPOSE_ARGS=(--env-file .env -f "$COMPOSE_FILE")
if [ -f docker-compose.ssl.yml ] && [ -f ssl/fullchain.pem ] && [ -f ssl/privkey.pem ]; then
  echo "[deploy] SSL certificate detected; enabling HTTPS"
  COMPOSE_ARGS+=(-f docker-compose.ssl.yml)
else
  echo "[deploy] SSL certificate not detected; using HTTP only"
fi

echo "[deploy] docker version"
docker --version
docker compose version

echo "[deploy] validate compose"
docker compose "${COMPOSE_ARGS[@]}" config --quiet

echo "[deploy] pull images"
docker compose "${COMPOSE_ARGS[@]}" pull

wait_for_healthy() {
  local service="$1"
  local container="$2"
  local timeout="${3:-420}"
  local elapsed=0

  echo "[deploy] wait for ${service} (${container})"
  while [ "$elapsed" -lt "$timeout" ]; do
    local status
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"

    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      echo "[deploy] ${service} is ${status}"
      return 0
    fi

    echo "[deploy] ${service} status=${status:-missing}; waiting..."
    sleep 10
    elapsed=$((elapsed + 10))
  done

  echo "[deploy] ${service} did not become healthy in ${timeout}s"
  docker logs --tail 120 "$container" || true
  return 1
}

echo "[deploy] start infrastructure"
docker compose "${COMPOSE_ARGS[@]}" up -d --remove-orphans \
  postgres redis elasticsearch zookeeper kafka minio

wait_for_healthy "postgres" "kb-postgres" 180
wait_for_healthy "redis" "kb-redis" 120
wait_for_healthy "elasticsearch" "kb-elasticsearch" 600
wait_for_healthy "zookeeper" "kb-zookeeper" 240
wait_for_healthy "kafka" "kb-kafka" 420
wait_for_healthy "minio" "kb-minio" 180

echo "[deploy] start application services"
docker compose "${COMPOSE_ARGS[@]}" up -d --remove-orphans \
  api-gateway auth-api knowledge-mentor-api code-intelligence-api frontend

echo "[deploy] cleanup dangling images"
docker image prune -f >/dev/null || true

echo "[deploy] status"
docker compose "${COMPOSE_ARGS[@]}" ps
