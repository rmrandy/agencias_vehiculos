using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/fabrica/repuestos")]
public class FabricaRepuestosController : ControllerBase
{
    private readonly FabricaProxyService _fabrica;

    public FabricaRepuestosController(FabricaProxyService fabrica)
    {
        _fabrica = fabrica;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] long? categoryId, [FromQuery] long? brandId, CancellationToken ct)
    {
        var q = new List<string>();
        if (categoryId.HasValue) q.Add($"categoryId={categoryId}");
        if (brandId.HasValue) q.Add($"brandId={brandId}");
        var path = "/api/repuestos" + (q.Count > 0 ? "?" + string.Join("&", q) : "");
        var res = await _fabrica.GetAsync(path, ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    [HttpGet("busqueda")]
    public async Task<IActionResult> Busqueda(
        [FromQuery] string? nombre,
        [FromQuery] string? descripcion,
        [FromQuery] string? especificaciones,
        CancellationToken ct)
    {
        var q = new List<string>();
        if (!string.IsNullOrWhiteSpace(nombre)) q.Add($"nombre={Uri.EscapeDataString(nombre)}");
        if (!string.IsNullOrWhiteSpace(descripcion)) q.Add($"descripcion={Uri.EscapeDataString(descripcion)}");
        if (!string.IsNullOrWhiteSpace(especificaciones)) q.Add($"especificaciones={Uri.EscapeDataString(especificaciones)}");
        var path = "/api/repuestos/busqueda" + (q.Count > 0 ? "?" + string.Join("&", q) : "");
        var res = await _fabrica.GetAsync(path, ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var res = await _fabrica.GetAsync($"/api/repuestos/{id}", ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }
}
