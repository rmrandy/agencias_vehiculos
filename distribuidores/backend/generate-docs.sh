#!/usr/bin/env bash
# Genera el sitio DocFX en docs/_site. Ejecutar desde cualquier directorio.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/docs"
dotnet tool restore --tool-manifest "$SCRIPT_DIR/.config/dotnet-tools.json"
dotnet tool run docfx -- docfx.json
echo "Listo: abre file://$SCRIPT_DIR/docs/_site/index.html (o sirve esa carpeta con un servidor estático)."
