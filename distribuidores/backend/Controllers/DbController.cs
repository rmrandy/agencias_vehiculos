using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// Diagnóstico de conectividad a SQL Server (<c>GET /api/db</c>).
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class DbController : ControllerBase
{
    private readonly AppDbContext _db;

    /// <param name="db">Contexto EF Core inyectado.</param>
    public DbController(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Comprueba conectividad con SQL Server mediante el API de base de datos de Entity Framework Core.</summary>
    /// <param name="ct">Token de cancelación.</param>
    /// <returns>200 si hay conexión; 503 con mensaje de error en caso contrario.</returns>
    [HttpGet]
    public async Task<IActionResult> Get(CancellationToken ct)
    {
        try
        {
            await _db.Database.CanConnectAsync(ct);
            return Ok(new { status = "ok", database = "connected" });
        }
        catch (Exception ex)
        {
            return StatusCode(503, new { status = "error", database = ex.Message });
        }
    }
}
