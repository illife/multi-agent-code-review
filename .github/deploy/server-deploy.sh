#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/root/think}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.server-test.yml}"

cd "$APP_DIR"

echo "[deploy] docker version"
docker --version
docker compose version

echo "[deploy] validate compose"
docker compose --env-file .env -f "$COMPOSE_FILE" config --quiet

echo "[deploy] pull images"
docker compose --env-file .env -f "$COMPOSE_FILE" pull

echo "[deploy] start stack"
docker compose --env-file .env -f "$COMPOSE_FILE" up -d --remove-orphans

echo "[deploy] cleanup dangling images"
docker image prune -f >/dev/null || true

echo "[deploy] status"
docker compose --env-file .env -f "$COMPOSE_FILE" ps
