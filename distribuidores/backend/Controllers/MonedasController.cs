using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>Divisas de cobro y tipo de cambio (unidades de moneda por 1 USD). Lectura pública; tasas solo ADMIN.</summary>
[ApiController]
[Route("api/[controller]")]
public class MonedasController : ControllerBase
{
    private readonly MonedaService _moneda;
    private readonly AppDbContext _db;

    public MonedasController(MonedaService moneda, AppDbContext db)
    {
        _moneda = moneda;
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var list = await _moneda.ListActiveAsync(ct);
        return Ok(list.Select(m => new
        {
            code = m.Code,
            name = m.Name,
            symbol = m.Symbol,
            unitsPerUsd = m.UnitsPerUsd
        }));
    }

    [HttpPut("{code}")]
    public async Task<IActionResult> Update(string code, [FromBody] UpdateMonedaRequest body, CancellationToken ct)
    {
        if (body?.AdminUserId == null || !await IsAdminAsync(body.AdminUserId.Value, ct))
            return StatusCode(403, new { message = "Requiere rol ADMIN" });
        try
        {
            var row = await _moneda.UpdateUnitsPerUsdAsync(code, body.UnitsPerUsd, ct);
            return Ok(new
            {
                code = row!.Code,
                name = row.Name,
                symbol = row.Symbol,
                unitsPerUsd = row.UnitsPerUsd
            });
        }
        catch (ArgumentException e)
        {
            return BadRequest(new { message = e.Message });
        }
    }

    private async Task<bool> IsAdminAsync(long userId, CancellationToken ct)
    {
        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);
        return user?.UserRoles?.Any(ur => ur.Role?.Name == "ADMIN") ?? false;
    }
}

public class UpdateMonedaRequest
{
    public long? AdminUserId { get; set; }
    public decimal UnitsPerUsd { get; set; }
}
