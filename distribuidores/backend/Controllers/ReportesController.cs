using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>Envía eventos de engagement a la fábrica (visto en detalle, agregado al carrito).</summary>
[ApiController]
[Route("api/reportes")]
public class ReportesController : ControllerBase
{
    private readonly FabricaProxyService _fabrica;
    private readonly PartService _partService;

    public ReportesController(FabricaProxyService fabrica, PartService partService)
    {
        _fabrica = fabrica;
        _partService = partService;
    }

    /// <summary>Registra en la fábrica que se vio el detalle de un repuesto. El frontend llama con partId (local); resolvemos partNumber para la fábrica.</summary>
    [HttpPost("visto-detalle")]
    public async Task<IActionResult> VistoDetalle([FromBody] ReporteEventRequest body, CancellationToken ct)
    {
        if (body?.PartId == null) return BadRequest(new { message = "partId es obligatorio" });
        var part = await _partService.GetByIdAsync(body.PartId.Value, ct);
        var payload = new
        {
            partNumber = part?.PartNumber ?? body.PartNumber,
            userId = body.UserId,
            clientType = body.ClientType ?? "PARTICULAR",
            source = body.Source ?? "distribuidores"
        };
        var res = await _fabrica.PostAsync("/api/reporteria/visto-detalle", payload, ct);
        if (!res.IsSuccessStatusCode)
            return StatusCode((int)res.StatusCode, new { message = "No se pudo registrar en la fábrica" });
        return StatusCode(201, new { ok = true });
    }

    /// <summary>Registra en la fábrica que se agregó un repuesto al carrito (sin compra).</summary>
    [HttpPost("agregado-carrito")]
    public async Task<IActionResult> AgregadoCarrito([FromBody] ReporteEventRequest body, CancellationToken ct)
    {
        if (body?.PartId == null) return BadRequest(new { message = "partId es obligatorio" });
        var part = await _partService.GetByIdAsync(body.PartId.Value, ct);
        var payload = new
        {
            partNumber = part?.PartNumber ?? body.PartNumber,
            userId = body.UserId,
            clientType = body.ClientType ?? "PARTICULAR",
            source = body.Source ?? "distribuidores"
        };
        var res = await _fabrica.PostAsync("/api/reporteria/agregado-carrito", payload, ct);
        if (!res.IsSuccessStatusCode)
            return StatusCode((int)res.StatusCode, new { message = "No se pudo registrar en la fábrica" });
        return StatusCode(201, new { ok = true });
    }
}

public class ReporteEventRequest
{
    public long? PartId { get; set; }
    public string? PartNumber { get; set; }
    public long? UserId { get; set; }
    public string? ClientType { get; set; }
    public string? Source { get; set; }
}
