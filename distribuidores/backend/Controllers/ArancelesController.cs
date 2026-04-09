using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>Aranceles por país LATAM (destino del envío). Lectura pública; edición solo ADMIN.</summary>
[ApiController]
[Route("api/[controller]")]
public class ArancelesController : ControllerBase
{
    private readonly ArancelService _arancel;
    private readonly ShippingRateService _shippingRate;
    private readonly AppDbContext _db;

    public ArancelesController(ArancelService arancel, ShippingRateService shippingRate, AppDbContext db)
    {
        _arancel = arancel;
        _shippingRate = shippingRate;
        _db = db;
    }

    /// <summary>Lista países LATAM con su porcentaje de arancel actual.</summary>
    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var list = await _arancel.ListAsync(ct);
        return Ok(list.Select(a => new
        {
            countryCode = a.CountryCode,
            countryName = a.CountryName,
            tariffPercent = a.TariffPercent
        }));
    }

    /// <summary>Tarifa global de envío (USD por libra de peso total del pedido). Lectura pública.</summary>
    [HttpGet("envio/tarifa-por-libra")]
    public async Task<IActionResult> GetTarifaEnvioPorLibra(CancellationToken ct)
    {
        var usdPerLb = await _shippingRate.GetUsdPerLbAsync(ct);
        return Ok(new { usdPerLb });
    }

    /// <summary>Actualiza la tarifa de envío por libra (solo ADMIN).</summary>
    [HttpPut("envio/tarifa-por-libra")]
    public async Task<IActionResult> PutTarifaEnvioPorLibra([FromBody] UpdateTarifaEnvioRequest body, CancellationToken ct)
    {
        if (body?.AdminUserId == null || !await IsAdminAsync(body.AdminUserId.Value, ct))
            return StatusCode(403, new { message = "Requiere rol ADMIN" });
        try
        {
            var row = await _shippingRate.SetUsdPerLbAsync(body.UsdPerLb, ct);
            return Ok(new { usdPerLb = row.UsdPerLb });
        }
        catch (ArgumentException e)
        {
            return BadRequest(new { message = e.Message });
        }
    }

    /// <summary>Catálogo de países válidos (sin porcentajes) para formularios.</summary>
    [HttpGet("paises")]
    public IActionResult PaisesLatam()
    {
        return Ok(LatamCountries.All.OrderBy(kv => kv.Value).Select(kv => new
        {
            countryCode = kv.Key,
            countryName = kv.Value
        }));
    }

    /// <summary>Actualiza el arancel de un país (solo ADMIN).</summary>
    [HttpPut("{countryCode}")]
    public async Task<IActionResult> Update(string countryCode, [FromBody] UpdateArancelRequest body, CancellationToken ct)
    {
        if (body?.AdminUserId == null || !await IsAdminAsync(body.AdminUserId.Value, ct))
            return StatusCode(403, new { message = "Requiere rol ADMIN" });
        try
        {
            var row = await _arancel.UpdateAsync(countryCode, body.TariffPercent, ct);
            return Ok(new { countryCode = row!.CountryCode, countryName = row.CountryName, tariffPercent = row.TariffPercent });
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

public class UpdateArancelRequest
{
    public long? AdminUserId { get; set; }
    public decimal TariffPercent { get; set; }
}

public class UpdateTarifaEnvioRequest
{
    public long? AdminUserId { get; set; }
    public decimal UsdPerLb { get; set; }
}
