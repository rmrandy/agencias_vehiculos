# Frontend Fábrica (Vue)

Frontend Vue 3 + Vite para el sistema de la fábrica. Se conecta al backend Java en `http://localhost:8080`.

## Dónde está este proyecto (evitar `cd: no such file`)

Este `frontend` es el de **fábrica**, no el de distribuidora. La ruta completa suele ser:

`…/agencias_vehiculos/fabrica/frontend`

- Si tu terminal ya muestra algo como `…/fabrica/frontend %`, **no** ejecutes `cd fabrica/frontend` (eso solo sirve desde la **raíz del repo** `agencias_vehiculos`).
- Si estás en `…/distribuidores/frontend`, ve a fábrica con:

  `cd ../../fabrica/frontend`

- Desde la raíz del repo:

  `cd fabrica/frontend`

## Requisitos

- Node.js 18+
- Backend fábrica corriendo en el puerto 8080

## Configuración

Copia `.env` o crea uno con la URL del API:

```
VITE_API_URL=http://localhost:8080
```

## Instalación y ejecución

```bash
npm install
npm run dev
```

Abre http://localhost:5173. El Home muestra el estado del backend (endpoint `/api/health`).

## Pruebas unitarias (Node nativo)

No usan Vitest: se ejecutan con **`node --test`** (incluido en Node.js 18+). No añaden dependencias extra al `npm install`.

```bash
npm install
npm run test
```

Los archivos son `src/**/*.test.js` y usan `node:test` + `node:assert/strict`; donde hace falta se reemplaza temporalmente `globalThis.fetch`.

## Estructura

- `src/views/Home.vue` – Página principal con comprobación de salud del API
- `src/api/health.js` – Cliente para el endpoint de health del backend
