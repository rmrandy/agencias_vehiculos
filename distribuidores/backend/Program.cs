using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;

var builder = WebApplication.CreateBuilder(args);

// Connection string: puede sobrescribirse con variable de entorno ConnectionStrings__DefaultConnection
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? "Server=localhost,1433;Database=AgenciasDistribuidores;User Id=sa;Password=YourPassword123;TrustServerCertificate=True;";

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddScoped<BackendDistribuidores.Services.AuthService>();
builder.Services.AddScoped<BackendDistribuidores.Services.PartService>();
builder.Services.AddScoped<BackendDistribuidores.Services.OrderService>();
builder.Services.AddScoped<BackendDistribuidores.Services.MailService>();
builder.Services.AddHttpClient<BackendDistribuidores.Services.FabricaProxyService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

// Puerto configurable: variable de entorno PORT o Server:Port en appsettings (por defecto 5080)
var port = Environment.GetEnvironmentVariable("PORT") ?? builder.Configuration["Server:Port"] ?? "5080";
builder.WebHost.UseUrls($"http://localhost:{port}");

var app = builder.Build();

app.UseCors();
app.UseHttpsRedirection();
app.MapControllers();

// Crear la base de datos y tablas si no existen (sin migraciones) y seed inicial
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await db.Database.EnsureCreatedAsync();
    await SeedData.EnsureSeedAsync(db);
}

app.Run();
