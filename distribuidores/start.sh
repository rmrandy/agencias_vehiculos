#!/usr/bin/env bash
# Levanta backend (.NET) y frontend (React) del sistema Distribuidora.
# Uso: ./start.sh [puerto_backend]
#   Ejemplo: ./start.sh 5050  → Backend en 5050, Frontend en 5051
#   Ejemplo: ./start.sh 5060  → Backend en 5060, Frontend en 5061
# Si no pasas puerto, se usa 5050.

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BE_PORT="${1:-5050}"
FE_PORT="$((BE_PORT + 1))"

echo "=== Distribuidora: Backend :$BE_PORT | Frontend :$FE_PORT ==="
echo "API URL del frontend: http://localhost:$BE_PORT"
echo ""

cleanup() {
  echo ""
  echo "Cerrando backend (PID $BACKEND_PID)..."
  kill "$BACKEND_PID" 2>/dev/null || true
  exit 0
}
trap cleanup INT TERM

# Backend en segundo plano
cd "$SCRIPT_DIR/backend"
PORT="$BE_PORT" dotnet run &
BACKEND_PID=$!
cd "$SCRIPT_DIR"

sleep 5

# Frontend en primer plano
cd "$SCRIPT_DIR/frontend"
VITE_API_URL="http://localhost:$BE_PORT" npm run dev -- --port "$FE_PORT"
cleanup
