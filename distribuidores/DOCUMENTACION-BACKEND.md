# Documentación — Backend Distribuidora

API REST del sistema de **distribuidores** en **.NET 9**, **Entity Framework Core** y **SQL Server**. Expone catálogo local, pedidos, usuarios y un **proxy** opcional hacia el API de la fábrica.

---

## Stack y requisitos

| Componente | Versión / notas |
|------------|-----------------|
| Runtime | .NET 9 SDK |
| Base de datos | SQL Server |
| ORM | EF Core (`AppDbContext`) |

---

## Arranque y configuración

### Cadena de conexión

Se lee de `ConnectionStrings:DefaultConnection` o de la variable de entorno `ConnectionStrings__DefaultConnection`. Si falta, se usa un valor por defecto orientado a desarrollo local (ver `Program.cs`).

### Puerto

Por defecto **5080**. Se puede cambiar con `PORT` o `Server:Port` en configuración:

```csharp
// Puerto configurable: variable de entorno PORT o Server:Port en appsettings (por defecto 5080)
var port = Environment.GetEnvironmentVariable("PORT") ?? builder.Configuration["Server:Port"] ?? "5080";
builder.WebHost.UseUrls($"http://localhost:{port}");
```

(Fuente: `distribuidores/backend/Program.cs`.)

### Comandos

```bash
cd distribuidores/backend
dotnet run
```

Comprobar estado:

```bash
curl http://localhost:5080/api/health
```

---

## Arquitectura en código

### Registro de servicios

En `Program.cs` se registran el contexto de base de datos, servicios de dominio (auth, repuestos, pedidos, correo), clientes HTTP para integración con fábrica y generación de PDF de recibos:

```csharp
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddScoped<BackendDistribuidores.Services.AuthService>();
builder.Services.AddScoped<BackendDistribuidores.Services.PartService>();
builder.Services.AddScoped<BackendDistribuidores.Services.OrderService>();
builder.Services.AddScoped<BackendDistribuidores.Services.MailService>();
builder.Services.AddHttpClient<BackendDistribuidores.Services.FabricaProxyService>();
// ... FabricaIntegrationService, UnifiedCatalogService, PedidoReciboPdfService

builder.Services.AddControllers();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});
```

### Inicialización de base de datos

Al arrancar se crea el esquema si no existe y se ejecuta el seed (sin migraciones explícitas):

```csharp
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await db.Database.EnsureCreatedAsync();
    await SeedData.EnsureSeedAsync(db);
}
```

### Ejemplo de controlador REST

Los controladores viven en `Controllers/` y usan rutas bajo `api/...`. Ejemplo de autenticación local:

```csharp
[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AuthService _auth;

    public AuthController(AuthService auth)
    {
        _auth = auth;
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Password))
            return BadRequest(new { message = "Email y contraseña son obligatorios" });

        var user = await _auth.LoginAsync(request.Email, request.Password, ct);
        if (user == null)
            return Unauthorized(new { message = "Credenciales incorrectas" });

        var roles = user.UserRoles?.Select(ur => ur.Role?.Name).Where(n => n != null).Cast<string>().ToList() ?? new List<string>();
        return Ok(new
        {
            userId = user.UserId,
            email = user.Email,
            fullName = user.FullName,
            phone = user.Phone,
            status = user.Status,
            roles
        });
    }
    // ... register
}
```

(Fuente: `distribuidores/backend/Controllers/AuthController.cs`.)

---

## Mapa de controladores (rutas principales)

| Prefijo | Controlador | Rol |
|---------|---------------|-----|
| `api/auth` | `AuthController` | Login/registro local |
| `api/repuestos` | `RepuestosController` | Catálogo (incl. integración unificada según configuración) |
| `api/pedidos` | `PedidosController` | Creación y consulta de pedidos |
| `api/categorias`, `api/marcas` | `CategoriasController`, `MarcasController` | Metadatos de catálogo |
| `api/images` | `ImagesController` | Imágenes de repuestos |
| `api/fabrica/*` | `FabricaAuthController`, `FabricaRepuestosController`, `FabricaPedidosController` | Proxy hacia API de fábrica |
| `api/reportes` | `ReportesController` | Reportes que combinan datos locales/proxy |
| `api/health` | `HealthController` | Salud del servicio |
| `api/db` | `DbController` | Prueba de conexión SQL Server |
| `api/distribuidores` | `DistribuidoresController` | CRUD legacy de distribuidores |
| `api/usuarios`, `api/proveedores` | `UsuariosController`, `ProveedoresController` | Administración |

La lista detallada de métodos HTTP y ejemplos `curl` está en `distribuidores/backend/README.md`.

---

## Integración con la fábrica

El distribuidor puede llamar a la fábrica mediante `FabricaProxyService` / `FabricaIntegrationService` (URLs de proveedor configuradas en base de datos). El frontend de la distribuidora debe apuntar siempre al **backend .NET** (`VITE_API_URL`, típicamente puerto 5080), no directamente al Jetty de la fábrica, salvo en flujos explícitos de administración de proveedores.

---

## Pruebas

Proyecto de tests: `distribuidores/BackendDistribuidores.Tests/` (xUnit).

```bash
cd distribuidores/BackendDistribuidores.Tests
dotnet test
```

---

## Referencias en el repositorio

- `distribuidores/backend/Program.cs` — bootstrap y DI  
- `distribuidores/backend/Controllers/` — API REST  
- `distribuidores/backend/Data/AppDbContext.cs` — modelo EF  
- `distribuidores/backend/Services/` — lógica de negocio  
- `distribuidores/backend/README.md` — endpoints y variables de entorno ampliados  
