using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Data;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/marcas")]
public class MarcasController : ControllerBase
{
    private readonly AppDbContext _db;

    public MarcasController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var list = await _db.Brands.OrderBy(b => b.Name).ToListAsync(ct);
        return Ok(list.Select(b => new { brandId = b.BrandId, name = b.Name }));
    }

    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var b = await _db.Brands.FindAsync(new object[] { id }, ct);
        if (b == null) return NotFound();
        return Ok(new { brandId = b.BrandId, name = b.Name });
    }
}
