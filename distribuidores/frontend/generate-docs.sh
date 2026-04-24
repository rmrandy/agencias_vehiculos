#!/usr/bin/env bash
# Genera la referencia TypeDoc en docs/_site. Ejecutar desde cualquier directorio.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
if [[ ! -d node_modules ]]; then
  npm install
fi
npm run docs
echo "Listo: abre file://$SCRIPT_DIR/docs/_site/index.html (o: cd docs/_site && npx --yes serve -l 3456)."
