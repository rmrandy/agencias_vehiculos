# Frontend Fábrica (Vue)

Frontend Vue 3 + Vite para el sistema de la fábrica. Se conecta al backend Java en `http://localhost:8080`.

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

## Estructura

- `src/views/Home.vue` – Página principal con comprobación de salud del API
- `src/api/health.js` – Cliente para el endpoint de health del backend
