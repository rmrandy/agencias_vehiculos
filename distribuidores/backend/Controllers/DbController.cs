using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/[controller]")]
public class DbController : ControllerBase
{
    private readonly AppDbContext _db;

    public DbController(AppDbContext db)
    {
        _db = db;
    }

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
