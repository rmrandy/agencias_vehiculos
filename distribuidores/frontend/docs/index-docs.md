# Frontend Distribuidores

Sitio generado con [TypeDoc](https://typedoc.org/) a partir del código **TypeScript/React** (`src/`).

Incluye módulos `api/`, contextos, componentes y vistas exportados. Los comentarios `/** ... */` sobre funciones y constantes aparecen en la referencia.

## Generar de nuevo el sitio

La salida va a **`docs/_site/`** (no confundir con esta carpeta `docs/` donde está este archivo y `typedoc.json` apunta a `out: docs/_site` desde la raíz del frontend).

**Opción recomendada** (desde la raíz del repo `agencias_vehiculos`):

```bash
./distribuidores/frontend/generate-docs.sh
```

**Manual:** sitúate en `distribuidores/frontend` (donde está `package.json`) y ejecuta `npm run docs`. Requiere Node.js 18+.
