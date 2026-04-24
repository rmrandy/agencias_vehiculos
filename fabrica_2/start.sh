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
# Fuerza credenciales de fábrica_2 para evitar que variables de entorno heredadas
# (p. ej. DB_USER=FABRICA de otra terminal/proyecto) apunten al esquema incorrecto.
cd "$SCRIPT_DIR/backend"
DB_USER="FABRICA2" DB_PASS="123" PORT="$BE_PORT" mvn -q exec:java -Dexec.mainClass="com.agencias.backend.Main" &
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
FE_DIR="$SCRIPT_DIR/frontend"
cd "$FE_DIR"
if [ ! -f "node_modules/vite/package.json" ]; then
  echo ""
  echo "Error: no hay dependencias del frontend (falta node_modules/vite)."
  echo "Instálalas una vez y vuelve a ejecutar este script:"
  echo "  cd \"$FE_DIR\" && npm install"
  echo ""
  kill "$BACKEND_PID" 2>/dev/null || true
  exit 1
fi
# npm exec asegura usar el vite local (evita \"vite: command not found\" si el PATH del script no incluye .bin)
VITE_API_URL="http://localhost:$BE_PORT" npm exec -- vite --port "$FE_PORT"
# Cuando se cierra el frontend (Ctrl+C), cleanup mata el backend
cleanup
