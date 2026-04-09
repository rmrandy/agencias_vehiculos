using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Data;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

/// <summary>Consulta de marcas de repuestos (<c>GET /api/marcas</c>).</summary>
[ApiController]
[Route("api/marcas")]
public class MarcasController : ControllerBase
{
    private readonly AppDbContext _db;

    public MarcasController(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Lista todas las marcas ordenadas por nombre.</summary>
    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var list = await _db.Brands.OrderBy(b => b.Name).ToListAsync(ct);
        return Ok(list.Select(b => new { brandId = b.BrandId, name = b.Name }));
    }

    /// <summary>Obtiene una marca por id.</summary>
    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var b = await _db.Brands.FindAsync(new object[] { id }, ct);
        if (b == null) return NotFound();
        return Ok(new { brandId = b.BrandId, name = b.Name });
    }
}
