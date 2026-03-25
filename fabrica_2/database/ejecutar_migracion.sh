#!/bin/bash

# Script para ejecutar la migración de inventario
# Uso: ./ejecutar_migracion.sh

echo "🔧 Ejecutando migración de inventario..."
echo ""

# Conectar a Oracle y ejecutar el script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
sqlplus -S FABRICA2/123@localhost:1521/XEPDB1 <<EOF
@${SCRIPT_DIR}/07_add_inventory_fields.sql
EXIT;
EOF

echo ""
echo "✅ Migración completada!"
echo ""
echo "Ahora reinicia el backend para que funcione correctamente."
