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

## Estructura

- `src/views/Home.tsx` – Página principal con comprobación de salud del API
- `src/api/health.ts` – Cliente tipado para el endpoint de health del backend
