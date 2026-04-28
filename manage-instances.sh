#!/usr/bin/env bash
set -euo pipefail

# Gestor interactivo de instancias del stack agencias_vehiculos.
# Permite levantar / apagar fabricas y distribuidoras una por una.
#
# Uso:
#   ./manage-instances.sh                  → menú interactivo
#   ./manage-instances.sh up fabrica 1     → levanta fábrica 1 (Oracle compartido + BE + FE)
#   ./manage-instances.sh up dist 1        → levanta distribuidora 1 (SQL + BE + FE)
#   ./manage-instances.sh down fabrica 1   → apaga fábrica 1
#   ./manage-instances.sh down dist 1      → apaga distribuidora 1
#   ./manage-instances.sh status           → muestra estado actual
#   ./manage-instances.sh stop-all         → apaga TODO el stack y borra volúmenes

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-.env.instances}"
COMPOSE_FILE="docker-compose.instances.yml"

if [ ! -f "$ROOT_DIR/$ENV_FILE" ]; then
  echo "No existe $ENV_FILE en $ROOT_DIR"
  echo "Crea uno con: cp .env.instances.example .env.instances"
  exit 1
fi

detect_host_ip() {
  # 1) HOST_IP en el entorno o .env tiene prioridad
  if [ -n "${HOST_IP_OVERRIDE:-}" ]; then
    echo "$HOST_IP_OVERRIDE"
    return
  fi
  local ip=""
  if command -v ipconfig >/dev/null 2>&1; then
    ip=$(ipconfig getifaddr en0 2>/dev/null || true)
    [ -z "$ip" ] && ip=$(ipconfig getifaddr en1 2>/dev/null || true)
  fi
  if [ -z "$ip" ]; then
    ip=$(hostname -I 2>/dev/null | awk '{print $1}')
  fi
  if [ -z "$ip" ]; then
    ip="localhost"
  fi
  echo "$ip"
}

# Carga puertos del .env para imprimir URLs
set -a
# shellcheck source=/dev/null
source "$ROOT_DIR/$ENV_FILE"
set +a

# Determina la IP a publicar a los frontends y a la integración fábrica/distribuidora
export HOST_IP="${HOST_IP:-$(detect_host_ip)}"

DC=(docker compose -f "$ROOT_DIR/$COMPOSE_FILE" --env-file "$ROOT_DIR/$ENV_FILE")

fabrica_be_port() {
  case "$1" in
    1) echo "${FABRICA_1_BE_PORT:-5051}" ;;
    2) echo "${FABRICA_2_BE_PORT:-5061}" ;;
    3) echo "${FABRICA_3_BE_PORT:-5071}" ;;
    4) echo "${FABRICA_4_BE_PORT:-5081}" ;;
    5) echo "${FABRICA_5_BE_PORT:-5091}" ;;
  esac
}
fabrica_fe_port() {
  case "$1" in
    1) echo "${FABRICA_1_FE_PORT:-5052}" ;;
    2) echo "${FABRICA_2_FE_PORT:-5062}" ;;
    3) echo "${FABRICA_3_FE_PORT:-5072}" ;;
    4) echo "${FABRICA_4_FE_PORT:-5082}" ;;
    5) echo "${FABRICA_5_FE_PORT:-5092}" ;;
  esac
}
dist_be_port() {
  case "$1" in
    1) echo "${DIST_1_BE_PORT:-5180}" ;;
    2) echo "${DIST_2_BE_PORT:-5280}" ;;
  esac
}
dist_fe_port() {
  case "$1" in
    1) echo "${DIST_1_FE_PORT:-5273}" ;;
    2) echo "${DIST_2_FE_PORT:-5373}" ;;
  esac
}

services_for() {
  # $1 = fabrica|dist, $2 = N
  case "$1" in
    fabrica) echo "oracle-shared fabrica-be-$2 fabrica-fe-$2" ;;
    dist)    echo "sqlserver-d$2 dist-be-$2 dist-fe-$2" ;;
  esac
}

up_instance() {
  local kind="$1" n="$2"
  local svcs
  svcs="$(services_for "$kind" "$n")"
  if [ -z "$svcs" ]; then
    echo "Combinación inválida: $kind $n"
    return 1
  fi
  echo "Levantando $kind #$n  →  $svcs"
  # shellcheck disable=SC2086
  "${DC[@]}" up -d --build $svcs

  echo ""
  echo "Estado:"
  # shellcheck disable=SC2086
  "${DC[@]}" ps $svcs

  echo ""
  if [ "$kind" = "fabrica" ]; then
    echo "Fabrica #$n FE: http://$HOST_IP:$(fabrica_fe_port "$n")"
    echo "Fabrica #$n BE: http://$HOST_IP:$(fabrica_be_port "$n")/api/health"
  else
    echo "Distribuidora #$n FE: http://$HOST_IP:$(dist_fe_port "$n")"
    echo "Distribuidora #$n BE: http://$HOST_IP:$(dist_be_port "$n")/api/health"
  fi
  echo "Login: admin@admin.com / 123456"
}

down_instance() {
  local kind="$1" n="$2"
  local svcs
  if [ "$kind" = "fabrica" ]; then
    # No apagar oracle-shared al bajar una sola fábrica.
    svcs="fabrica-be-$n fabrica-fe-$n"
  else
    svcs="$(services_for "$kind" "$n")"
  fi
  if [ -z "$svcs" ]; then
    echo "Combinación inválida: $kind $n"
    return 1
  fi
  echo "Apagando $kind #$n  →  $svcs"
  # shellcheck disable=SC2086
  "${DC[@]}" stop $svcs
  # shellcheck disable=SC2086
  "${DC[@]}" rm -f $svcs
}

show_status() {
  "${DC[@]}" ps
}

stop_all() {
  echo "Apagando TODO el stack y borrando volúmenes..."
  "${DC[@]}" down -v
}

interactive_menu() {
  while true; do
    echo ""
    echo "===== Gestor de instancias agencias_vehiculos ====="
    echo " IP host: $HOST_IP"
    echo "  1) Levantar Fabrica  (1-5)"
    echo "  2) Levantar Distribuidora (1-2)"
    echo "  3) Apagar Fabrica  (1-5)"
    echo "  4) Apagar Distribuidora (1-2)"
    echo "  5) Ver estado"
    echo "  6) Apagar TODO el stack (con volúmenes)"
    echo "  0) Salir"
    echo "==================================================="
    read -r -p "Elige una opción: " opt
    case "$opt" in
      1)
        read -r -p "Número de fábrica (1-5): " n
        up_instance fabrica "$n"
        ;;
      2)
        read -r -p "Número de distribuidora (1-2): " n
        up_instance dist "$n"
        ;;
      3)
        read -r -p "Número de fábrica a apagar (1-5): " n
        down_instance fabrica "$n"
        ;;
      4)
        read -r -p "Número de distribuidora a apagar (1-2): " n
        down_instance dist "$n"
        ;;
      5) show_status ;;
      6) stop_all ;;
      0) exit 0 ;;
      *) echo "Opción inválida" ;;
    esac
  done
}

# Modo no interactivo
case "${1:-}" in
  up)
    up_instance "${2:?fabrica|dist}" "${3:?numero}"
    ;;
  down)
    down_instance "${2:?fabrica|dist}" "${3:?numero}"
    ;;
  status)
    show_status
    ;;
  stop-all)
    stop_all
    ;;
  "" )
    interactive_menu
    ;;
  *)
    echo "Uso:"
    echo "  $0                            → menú interactivo"
    echo "  $0 up fabrica <1-5>"
    echo "  $0 up dist <1-2>"
    echo "  $0 down fabrica <1-5>"
    echo "  $0 down dist <1-2>"
    echo "  $0 status"
    echo "  $0 stop-all"
    exit 1
    ;;
esac
