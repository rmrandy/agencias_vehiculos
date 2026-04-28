#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-.env.instances}"
COMPOSE_FILE="docker-compose.instances.yml"

if [ ! -f "$ROOT_DIR/$ENV_FILE" ]; then
  echo "No existe $ENV_FILE en $ROOT_DIR"
  echo "Crea uno con: cp .env.instances.example .env.instances"
  exit 1
fi

detect_host_ip() {
  if [ -n "${HOST_IP_OVERRIDE:-}" ]; then
    echo "$HOST_IP_OVERRIDE"; return
  fi
  local ip=""
  if command -v ipconfig >/dev/null 2>&1; then
    ip=$(ipconfig getifaddr en0 2>/dev/null || true)
    [ -z "$ip" ] && ip=$(ipconfig getifaddr en1 2>/dev/null || true)
  fi
  [ -z "$ip" ] && ip=$(hostname -I 2>/dev/null | awk '{print $1}')
  [ -z "$ip" ] && ip="localhost"
  echo "$ip"
}
export HOST_IP="${HOST_IP:-$(detect_host_ip)}"
echo "HOST_IP detectado: $HOST_IP"

echo "Levantando stack minimo: fabrica 1 + distribuidora 1..."
docker compose -f "$ROOT_DIR/$COMPOSE_FILE" --env-file "$ROOT_DIR/$ENV_FILE" up -d --build \
  oracle-shared fabrica-be-1 fabrica-fe-1 sqlserver-d1 dist-be-1 dist-fe-1

echo ""
echo "Listo. Estado:"
docker compose -f "$ROOT_DIR/$COMPOSE_FILE" --env-file "$ROOT_DIR/$ENV_FILE" ps
