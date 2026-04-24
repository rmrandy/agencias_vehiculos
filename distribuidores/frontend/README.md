# Frontend Distribuidora (React + TypeScript)

Frontend React 18 + TypeScript + Vite para el sistema de la distribuidora. Se conecta al backend .NET en `http://localhost:5080`.

## Requisitos

- Node.js 18+
- Backend distribuidora corriendo en el puerto 5080

## Configuración

Copia `.env` o crea uno con la URL del API:

```
VITE_API_URL=http://localhost:5080
```

## Instalación y ejecución

```bash
npm install
npm run dev
```

Abre http://localhost:5173. El Home muestra el estado del backend (endpoint `/api/health`).

## Documentación del código (TypeDoc)

Equivale a la referencia generada desde comentarios TSDoc/JSDoc (`/** ... */`) en `src/`, similar a Javadoc o al DocFX del backend .NET.

**Desde la raíz del repo** `agencias_vehiculos`:

```bash
./distribuidores/frontend/generate-docs.sh
```

**Manual** — debes estar en la carpeta del frontend (`distribuidores/frontend`), donde está `package.json`:

| Estás en… | Comando para ir al frontend |
|-----------|-------------------------------|
| Raíz del repo | `cd distribuidores/frontend` |
| Ya en `distribuidores` | `cd frontend` |

Luego:

```bash
npm install   # solo la primera vez o si falta node_modules
npm run docs
```

El HTML queda en **`distribuidores/frontend/docs/_site/index.html`**. Ábrelo en el navegador (`file://…`) o sirve la carpeta:

```bash
cd docs/_site && npx --yes serve -l 3456
```

y entra en http://localhost:3456

Configuración: `typedoc.json` y `tsconfig.typedoc.json`. Página de portada del sitio generado: `docs/index-docs.md`.

## Estructura

- `src/views/Home.tsx` – Página principal con comprobación de salud del API
- `src/api/health.ts` – Cliente tipado para el endpoint de health del backend
