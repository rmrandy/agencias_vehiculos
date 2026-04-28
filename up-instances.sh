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

echo "Levantando 5 fabricas + 2 distribuidoras..."
docker compose -f "$ROOT_DIR/$COMPOSE_FILE" --env-file "$ROOT_DIR/$ENV_FILE" up -d --build

echo ""
echo "Listo. Estado:"
docker compose -f "$ROOT_DIR/$COMPOSE_FILE" --env-file "$ROOT_DIR/$ENV_FILE" ps

# Cargar puertos para mostrar URLs finales de acceso HTTP.
set -a
# shellcheck source=/dev/null
source "$ROOT_DIR/$ENV_FILE"
set +a

echo ""
echo "Accesos HTTP (HOST_IP=$HOST_IP):"
echo "Fabrica 1 FE: http://$HOST_IP:${FABRICA_1_FE_PORT:-5052} | BE: http://$HOST_IP:${FABRICA_1_BE_PORT:-5051}"
echo "Fabrica 2 FE: http://$HOST_IP:${FABRICA_2_FE_PORT:-5062} | BE: http://$HOST_IP:${FABRICA_2_BE_PORT:-5061}"
echo "Fabrica 3 FE: http://$HOST_IP:${FABRICA_3_FE_PORT:-5072} | BE: http://$HOST_IP:${FABRICA_3_BE_PORT:-5071}"
echo "Fabrica 4 FE: http://$HOST_IP:${FABRICA_4_FE_PORT:-5082} | BE: http://$HOST_IP:${FABRICA_4_BE_PORT:-5081}"
echo "Fabrica 5 FE: http://$HOST_IP:${FABRICA_5_FE_PORT:-5092} | BE: http://$HOST_IP:${FABRICA_5_BE_PORT:-5091}"
echo "Distribuidora 1 FE: http://$HOST_IP:${DIST_1_FE_PORT:-5273} | BE: http://$HOST_IP:${DIST_1_BE_PORT:-5180}"
echo "Distribuidora 2 FE: http://$HOST_IP:${DIST_2_FE_PORT:-5373} | BE: http://$HOST_IP:${DIST_2_BE_PORT:-5280}"

echo ""
echo "Login inicial (todas las instancias): admin@admin.com / 123456"
