using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>Proxy de pedidos hacia el API de la fábrica (crear y consultar por usuario o id).</summary>
[ApiController]
[Route("api/fabrica/pedidos")]
public class FabricaPedidosController : ControllerBase
{
    private readonly FabricaProxyService _fabrica;

    public FabricaPedidosController(FabricaProxyService fabrica)
    {
        _fabrica = fabrica;
    }

    /// <summary>Crea pedido en la fábrica (cuerpo transparente al backend remoto).</summary>
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] object body, CancellationToken ct)
    {
        var res = await _fabrica.PostAsync("/api/pedidos", body, ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    /// <summary>Pedidos del usuario en la fábrica.</summary>
    [HttpGet("usuario/{userId:long}")]
    public async Task<IActionResult> GetByUser(long userId, CancellationToken ct)
    {
        var res = await _fabrica.GetAsync($"/api/pedidos/usuario/{userId}", ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    /// <summary>Detalle de pedido en la fábrica.</summary>
    [HttpGet("{orderId:long}")]
    public async Task<IActionResult> GetById(long orderId, CancellationToken ct)
    {
        var res = await _fabrica.GetAsync($"/api/pedidos/{orderId}", ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }
}
