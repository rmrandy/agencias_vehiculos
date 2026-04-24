using BackendDistribuidores.Data;
using BackendDistribuidores.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

/// <summary>Reportería comercial (admin / empleado) y reenvío de eventos de engagement a la fábrica.</summary>
[ApiController]
[Route("api/reportes")]
public class ReportesController : ControllerBase
{
    private readonly ReportesService _reportes;
    private readonly AppDbContext _db;
    private readonly FabricaProxyService _fabrica;

    public ReportesController(ReportesService reportes, AppDbContext db, FabricaProxyService fabrica)
    {
        _reportes = reportes;
        _db = db;
        _fabrica = fabrica;
    }

    /// <summary>Reenvía a la fábrica: repuesto visto en detalle (catálogo local).</summary>
    [HttpPost("visto-detalle")]
    public async Task<IActionResult> VistoDetalle([FromBody] EngagementFabricaRequest? body, CancellationToken ct)
    {
        if (body?.PartId == null || body.PartId <= 0)
            return BadRequest(new { message = "partId es obligatorio" });
        try
        {
            await _fabrica.PostAsync("/api/reporteria/visto-detalle", new
            {
                partId = body.PartId,
                userId = body.UserId,
                clientType = "WEB",
                source = "DISTRIBUIDORA"
            }, ct);
            return Ok();
        }
        catch
        {
            return Ok();
        }
    }

    /// <summary>Reenvía a la fábrica: repuesto agregado al carrito.</summary>
    [HttpPost("agregado-carrito")]
    public async Task<IActionResult> AgregadoCarrito([FromBody] EngagementFabricaRequest? body, CancellationToken ct)
    {
        if (body?.PartId == null || body.PartId <= 0)
            return BadRequest(new { message = "partId es obligatorio" });
        try
        {
            await _fabrica.PostAsync("/api/reporteria/agregado-carrito", new
            {
                partId = body.PartId,
                userId = body.UserId,
                clientType = "WEB",
                source = "DISTRIBUIDORA"
            }, ct);
            return Ok();
        }
        catch
        {
            return Ok();
        }
    }

    /// <summary>Repuestos locales más vendidos (excluye pedidos cancelados).</summary>
    [HttpGet("mas-vendidos")]
    public async Task<IActionResult> MasVendidos(
        [FromQuery] long userId,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to,
        [FromQuery] int top = 30,
        CancellationToken ct = default)
    {
        if (!await IsAdminOrEmployeeAsync(userId, ct))
            return Forbid();
        var data = await _reportes.GetMasVendidosAsync(from, to, top, ct);
        return Ok(data);
    }

    /// <summary>Pedidos por día (conteo e importe total), sin cancelados.</summary>
    [HttpGet("ventas-diarias")]
    public async Task<IActionResult> VentasDiarias(
        [FromQuery] long userId,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to,
        CancellationToken ct = default)
    {
        if (!await IsAdminOrEmployeeAsync(userId, ct))
            return Forbid();
        var data = await _reportes.GetVentasDiariasAsync(from, to, ct);
        return Ok(data);
    }

    /// <summary>Pedidos creados en el período agrupados por estado actual.</summary>
    [HttpGet("pedidos-por-estado")]
    public async Task<IActionResult> PedidosPorEstado(
        [FromQuery] long userId,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to,
        CancellationToken ct = default)
    {
        if (!await IsAdminOrEmployeeAsync(userId, ct))
            return Forbid();
        var data = await _reportes.GetPedidosPorEstadoAsync(from, to, ct);
        return Ok(data);
    }

    private async Task<bool> IsAdminOrEmployeeAsync(long userId, CancellationToken ct)
    {
        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);
        var names = user?.UserRoles?.Select(ur => ur.Role?.Name).Where(n => n != null).ToHashSet() ?? new HashSet<string?>();
        return names.Contains("ADMIN") || names.Contains("EMPLOYEE");
    }
}

public sealed class EngagementFabricaRequest
{
    public long? PartId { get; set; }
    public long? UserId { get; set; }
}
