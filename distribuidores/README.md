# Sistema Distribuidora

Backend (.NET) + Frontend (React + TypeScript) para el sistema distribuidora.

## Replicar el sistema en distintos puertos

Cada instancia usa un **par de puertos**: backend en `P`, frontend en `P+1`.

| Instancia | Backend (BE) | Frontend (FE) | Cómo levantar      |
|-----------|--------------|---------------|---------------------|
| 1         | 5050         | 5051          | `./start.sh 5050`  |
| 2         | 5060         | 5061          | `./start.sh 5060`  |
| 3         | 5070         | 5071          | `./start.sh 5070`  |

El frontend apunta al backend del mismo par (p. ej. FE 5051 → BE 5050).

## Un solo comando: Backend + Frontend

Desde la carpeta `distribuidores`:

```bash
./start.sh [puerto_backend]
```

- **Sin argumentos:** Backend en **5050**, Frontend en **5051**.
- **Con puerto:** Backend en el puerto indicado, Frontend en el siguiente.
  - `./start.sh 5050` → BE :5050, FE :5051
  - `./start.sh 5060` → BE :5060, FE :5061

Al hacer **Ctrl+C** se cierran ambos.

## Requisitos

- .NET 9 SDK (backend)
- Node.js 16+ (frontend)
- En `distribuidores/frontend`: `npm install` ya ejecutado

## Levantar solo backend o solo frontend

- **Solo backend** (puerto 5050 u otro):
  ```bash
  cd backend
  PORT=5050 dotnet run
  ```
- **Solo frontend** (que apunte a un backend concreto):
  ```bash
  cd frontend
  VITE_API_URL=http://localhost:5050 npm run dev -- --port 5051
  ```
