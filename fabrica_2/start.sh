#!/usr/bin/env bash
# Levanta backend (Java) y frontend (Vue) del sistema Fábrica.
# Uso: ./start.sh [puerto_backend]
#   Ejemplo: ./start.sh 8080  → Backend en 8080, Frontend en 8081
#   Ejemplo: ./start.sh 8090  → Backend en 8090, Frontend en 8091
# Si no pasas puerto, se usa 8080.

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BE_PORT="${1:-8080}"
FE_PORT="$((BE_PORT + 1))"

echo "=== Fábrica: Backend :$BE_PORT | Frontend :$FE_PORT ==="
echo "API URL del frontend: http://localhost:$BE_PORT"
echo ""

# Limpiar al salir (Ctrl+C o terminación)
cleanup() {
  echo ""
  echo "Cerrando backend (PID $BACKEND_PID)..."
  kill "$BACKEND_PID" 2>/dev/null || true
  exit 0
}
trap cleanup INT TERM

# Backend en segundo plano
cd "$SCRIPT_DIR/backend"
PORT="$BE_PORT" mvn -q exec:java -Dexec.mainClass="com.agencias.backend.Main" &
BACKEND_PID=$!
cd "$SCRIPT_DIR"

# Esperar a que el backend responda (hasta ~45 s); el Java tarda en iniciar (Hibernate, Oracle)
echo "Esperando a que el backend responda en :$BE_PORT ..."
MAX_WAIT=45
for i in $(seq 1 "$MAX_WAIT"); do
  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$BE_PORT/api/health" 2>/dev/null | grep -q 200; then
    echo "Backend listo."
    break
  fi
  if [ "$i" -eq "$MAX_WAIT" ]; then
    echo "Aviso: el backend no respondió a tiempo. Comprueba que Oracle esté encendido y que el puerto $BE_PORT esté libre."
  fi
  sleep 1
done

# Frontend (en primer plano; usa el puerto del backend para la API)
cd "$SCRIPT_DIR/frontend"
VITE_API_URL="http://localhost:$BE_PORT" npm run dev -- --port "$FE_PORT"
# Cuando se cierra el frontend (Ctrl+C), cleanup mata el backend
cleanup
