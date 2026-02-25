# Sistema Fábrica

Backend (Java) + Frontend (Vue) para el sistema de la fábrica.

## Replicar el sistema en distintos puertos

Cada instancia puede correr en un **par de puertos**: backend en `P`, frontend en `P+1`.

| Instancia | Backend (BE) | Frontend (FE) | Cómo levantar      |
|-----------|--------------|---------------|---------------------|
| 1         | 8080         | 8081          | `./start.sh 8080`  |
| 2         | 8090         | 8091          | `./start.sh 8090`  |
| 3         | 8100         | 8101          | `./start.sh 8100`  |

El frontend que levantes apuntará siempre al backend del mismo par (p. ej. FE 8081 → BE 8080).

## Un solo comando: Backend + Frontend

Desde la carpeta `fabrica`:

```bash
./start.sh [puerto_backend]
```

- **Sin argumentos:** Backend en **8080**, Frontend en **8081**.
- **Con puerto:** Backend en el puerto indicado, Frontend en el siguiente.
  - `./start.sh 8080` → BE :8080, FE :8081
  - `./start.sh 8090` → BE :8090, FE :8091

Al hacer **Ctrl+C** se cierran backend y frontend.

## Requisitos

- Java 17, Maven (para el backend)
- Node.js 16+ (para el frontend)
- En `fabrica/frontend`: `npm install` ya ejecutado

## Levantar solo backend o solo frontend

- **Solo backend** (puerto 8080 u otro):
  ```bash
  cd backend
  PORT=8080 mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
  ```
- **Solo frontend** (que apunte a un backend concreto):
  ```bash
  cd frontend
  VITE_API_URL=http://localhost:8080 npm run dev -- --port 8081
  ```
