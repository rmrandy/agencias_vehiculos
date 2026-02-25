# Backend .NET - Sistema de Distribuidores

API REST en .NET 9 con Entity Framework Core y SQL Server.

## Requisitos

- .NET 9 SDK
- SQL Server (local o en Docker)

## Configuración

### Cadena de conexión (SQL Server)

La conexión se configura en `appsettings.json` o con variables de entorno. Los valores de la imagen de DBeaver se traducen así:

| DBeaver / JDBC | .NET Connection String |
|----------------|------------------------|
| localhost:1433 | Server=localhost,1433 |
| databaseName=AgenciasDistribuidores | Database=AgenciasDistribuidores |
| trustServerCertificate=true | TrustServerCertificate=True |
| Usuario sa | User Id=sa |
| Contraseña | Password=TuPassword |

**appsettings.json** (ejemplo):

```json
"ConnectionStrings": {
  "DefaultConnection": "Server=localhost,1433;Database=AgenciasDistribuidores;User Id=sa;Password=TuPassword;TrustServerCertificate=True;"
}
```

**Variables de entorno** (tienen prioridad sobre appsettings):

```bash
# Cadena completa (recomendado para no dejar la contraseña en el repo)
export ConnectionStrings__DefaultConnection="Server=localhost,1433;Database=AgenciasDistribuidores;User Id=sa;Password=TuPassword;TrustServerCertificate=True;"

# Puerto del API (por defecto 5080)
export PORT=5080
```

### SQL Server en Docker

Si SQL Server corre en Docker, publica el puerto 1433:

```bash
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=TuPassword123" -p 1433:1433 -d mcr.microsoft.com/mssql/server:2022-latest
```

Luego usa `Server=localhost,1433` en la cadena de conexión.

## Ejecución

```bash
cd distribuidores/backend
dotnet run
```

Por defecto escucha en **http://localhost:5080**. Para otro puerto:

```bash
export PORT=8080
dotnet run
```

O:

```bash
dotnet run --urls "http://localhost:8080"
```

## Base de datos

El sistema usa **una base de datos propia** en SQL Server: **AgenciasDistribuidores** (DOC2: cada sistema con su propia BD). Al arrancar, si no existen las tablas se crean con `EnsureCreatedAsync` y se ejecuta el seed:

- **Roles:** USER, ADMIN  
- **Usuario de prueba:** `admin@distribuidor.local` / `123456`  
- **Catálogo:** categorías (Motor, Frenos, Suspensión, Eléctrico, Filtros, etc.), marcas (Bosch, Denso, NGK, Brembo, Mann Filter, Monroe) y repuestos dummy.

La estructura replica la lógica de la fábrica: CATEGORY, BRAND, PART, ORDER_HEADER, ORDER_ITEM, ORDER_STATUS_HISTORY, APP_USER, ROLE, USER_ROLE, PART_REVIEW, PROVEEDOR, más la tabla legacy Distribuidores.

## Endpoints

### Locales (catálogo y compra local)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | /api/auth/login | Login local (email + password). Devuelve userId, email, roles. |
| POST | /api/auth/register | Registro de usuario (opcional). |
| GET | /api/categorias | Lista categorías. |
| GET | /api/marcas | Lista marcas. |
| GET | /api/repuestos | Lista repuestos (opc. categoryId, brandId). |
| GET | /api/repuestos/busqueda | Búsqueda (q, nombre, descripcion, especificaciones). |
| GET | /api/repuestos/{id} | Un repuesto por ID. |
| GET | /api/images/part/{id} | Imagen del repuesto (si tiene). |
| POST | /api/pedidos | Crear pedido (body: userId, items: [{ partId, qty }]). |
| GET | /api/pedidos/usuario/{userId} | Pedidos del usuario. |
| GET | /api/pedidos/{orderId} | Detalle de un pedido. |

### Proxy a fábrica (opcional)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | /api/fabrica/auth/login | Login contra la fábrica (usuarios empresariales). |
| GET | /api/fabrica/repuestos | Repuestos de la fábrica. |
| GET/POST | /api/fabrica/pedidos | Pedidos en la fábrica. |

### Otros

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | /api | Info de la API y enlaces |
| GET | /api/health | Estado del servidor `{"status":"ok"}` |
| GET | /api/db | Comprueba conexión a SQL Server |
| GET | /api/distribuidores | Lista de distribuidores (legacy) |
| GET | /api/distribuidores/{id} | Un distribuidor por ID |
| POST | /api/distribuidores | Crear distribuidor |
| PUT | /api/distribuidores/{id} | Actualizar distribuidor |
| DELETE | /api/distribuidores/{id} | Eliminar distribuidor |

### Ejemplos con curl

```bash
# Health
curl http://localhost:5080/api/health

# Verificar conexión a SQL Server
curl http://localhost:5080/api/db

# Raíz de la API
curl http://localhost:5080/api

# Listar distribuidores
curl http://localhost:5080/api/distribuidores

# Crear distribuidor
curl -X POST http://localhost:5080/api/distribuidores \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Distribuidor Norte","contacto":"Juan Pérez","email":"juan@norte.com","telefono":"555-1234"}'

# Obtener por ID
curl http://localhost:5080/api/distribuidores/1
```

## Estructura del proyecto

```
distribuidores/backend/
├── Controllers/       # API (Health, Api, Db, Distribuidores)
├── Data/              # AppDbContext (EF Core)
├── Models/            # Entidad Distribuidor
├── appsettings.json   # Configuración y connection string
├── Program.cs         # Configuración de servicios y puerto
└── README.md
```

## Modelo Distribuidor

- **Id** (int, clave)
- **Nombre** (string, obligatorio)
- **Contacto** (string, opcional)
- **Email** (string, opcional)
- **Telefono** (string, opcional)

La tabla `Distribuidores` se crea automáticamente al iniciar la aplicación si no existe.
