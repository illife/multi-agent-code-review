#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/root/think}"

command -v docker >/dev/null 2>&1
docker compose version >/dev/null

mkdir -p "$APP_DIR"

cat >/etc/sysctl.d/99-think-platform.conf <<'EOF'
vm.max_map_count=262144
EOF
sysctl --system

if command -v ufw >/dev/null 2>&1; then
  ufw allow 22/tcp || true
  ufw allow 80/tcp || true
fi

echo "Server initialized for Think Platform at $APP_DIR"
