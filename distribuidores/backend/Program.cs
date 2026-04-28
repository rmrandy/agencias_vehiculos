using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;

// -----------------------------------------------------------------------------
// <summary>
// Host ASP.NET Core del sistema distribuidores (.NET 9): EF Core (SQL Server),
// controladores REST bajo /api, CORS abierto para desarrollo, EnsureCreated + seed al arrancar.
// Puerto: variable de entorno <c>PORT</c>, configuración <c>Server:Port</c> o 5080 por defecto.
// </summary>
// -----------------------------------------------------------------------------

var builder = WebApplication.CreateBuilder(args);

// Connection string: puede sobrescribirse con variable de entorno ConnectionStrings__DefaultConnection
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? "Server=localhost,1433;Database=AgenciasDistribuidores;User Id=sa;Password=YourPassword123;TrustServerCertificate=True;";

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddScoped<BackendDistribuidores.Services.AuthService>();
builder.Services.AddScoped<BackendDistribuidores.Services.ArancelService>();
builder.Services.AddScoped<BackendDistribuidores.Services.MonedaService>();
builder.Services.AddScoped<BackendDistribuidores.Services.ShippingRateService>();
builder.Services.AddScoped<BackendDistribuidores.Services.PartService>();
builder.Services.AddScoped<BackendDistribuidores.Services.OrderService>();
builder.Services.AddScoped<BackendDistribuidores.Services.ReportesService>();
builder.Services.AddScoped<BackendDistribuidores.Services.MailService>();
builder.Services.AddHttpClient<BackendDistribuidores.Services.FabricaProxyService>();
builder.Services.AddHttpClient("FabricaIntegration", client =>
{
    client.Timeout = TimeSpan.FromSeconds(120);
});
builder.Services.AddScoped<BackendDistribuidores.Services.FabricaIntegrationService>();
builder.Services.AddScoped<BackendDistribuidores.Services.UnifiedCatalogService>();
builder.Services.AddScoped<BackendDistribuidores.Services.PedidoReciboPdfService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
var corsAllowedOrigins = Environment.GetEnvironmentVariable("CORS_ALLOWED_ORIGINS");
builder.Services.AddCors(options =>
{
    options.AddPolicy("InstanceCors", policy =>
    {
        if (string.IsNullOrWhiteSpace(corsAllowedOrigins) || corsAllowedOrigins!.Trim() == "*")
        {
            policy.AllowAnyOrigin()
                  .AllowAnyMethod()
                  .AllowAnyHeader();
            return;
        }

        var origins = corsAllowedOrigins
            .Split(',', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);

        policy.WithOrigins(origins)
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

// Puerto configurable: variable de entorno PORT o Server:Port en appsettings (por defecto 5080)
// Importante en Docker: escuchar en 0.0.0.0 para exponer correctamente el contenedor.
var port = Environment.GetEnvironmentVariable("PORT") ?? builder.Configuration["Server:Port"] ?? "5080";
builder.WebHost.UseUrls($"http://0.0.0.0:{port}");

var app = builder.Build();

app.UseCors("InstanceCors");
// SPA: los assets del front (Vite) deben estar en wwwroot; p. ej. `npm run build` en
// `distribuidores/frontend` (el .csproj copia dist/ → wwwroot/ al compilar si index.html existe).
app.UseDefaultFiles();
app.UseStaticFiles();
app.MapControllers();
app.MapFallbackToFile("index.html");

// Crear la base de datos y tablas si no existen (sin migraciones) y seed inicial
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    var maxAttempts = 20;
    var delay = TimeSpan.FromSeconds(5);
    var ensured = false;

    // SQL Server en contenedor puede tardar en aceptar conexiones aunque el contenedor esté "running".
    for (var attempt = 1; attempt <= maxAttempts; attempt++)
    {
        try
        {
            await db.Database.EnsureCreatedAsync();
            ensured = true;
            break;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[Startup] EnsureCreated intento {attempt}/{maxAttempts} falló: {ex.Message}");
            if (attempt == maxAttempts)
            {
                throw;
            }
            await Task.Delay(delay);
        }
    }

    if (!ensured)
    {
        throw new InvalidOperationException("No se pudo inicializar la base de datos.");
    }
    await SeedData.EnsureSeedAsync(db);
}

app.Run();
