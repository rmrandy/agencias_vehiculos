using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;

var builder = WebApplication.CreateBuilder(args);

// Connection string: puede sobrescribirse con variable de entorno ConnectionStrings__DefaultConnection
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? "Server=localhost,1433;Database=master;User Id=sa;Password=YourPassword123;TrustServerCertificate=True;";

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

// Puerto configurable: variable de entorno PORT o Server:Port en appsettings (por defecto 5080)
var port = Environment.GetEnvironmentVariable("PORT") ?? builder.Configuration["Server:Port"] ?? "5080";
builder.WebHost.UseUrls($"http://localhost:{port}");

var app = builder.Build();

app.UseHttpsRedirection();
app.MapControllers();

// Crear la base de datos y tablas si no existen (sin migraciones)
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await db.Database.EnsureCreatedAsync();
}

app.Run();
