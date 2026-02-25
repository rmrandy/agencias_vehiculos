using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Data;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/categorias")]
public class CategoriasController : ControllerBase
{
    private readonly AppDbContext _db;

    public CategoriasController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var list = await _db.Categories.OrderBy(c => c.Name).ToListAsync(ct);
        return Ok(list.Select(c => new { categoryId = c.CategoryId, name = c.Name, parentId = c.ParentId }));
    }

    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var c = await _db.Categories.FindAsync(new object[] { id }, ct);
        if (c == null) return NotFound();
        return Ok(new { categoryId = c.CategoryId, name = c.Name, parentId = c.ParentId });
    }
}
