using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/fabrica/pedidos")]
public class FabricaPedidosController : ControllerBase
{
    private readonly FabricaProxyService _fabrica;

    public FabricaPedidosController(FabricaProxyService fabrica)
    {
        _fabrica = fabrica;
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] object body, CancellationToken ct)
    {
        var res = await _fabrica.PostAsync("/api/pedidos", body, ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    [HttpGet("usuario/{userId:long}")]
    public async Task<IActionResult> GetByUser(long userId, CancellationToken ct)
    {
        var res = await _fabrica.GetAsync($"/api/pedidos/usuario/{userId}", ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }

    [HttpGet("{orderId:long}")]
    public async Task<IActionResult> GetById(long orderId, CancellationToken ct)
    {
        var res = await _fabrica.GetAsync($"/api/pedidos/{orderId}", ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        return StatusCode((int)res.StatusCode, Content(content, "application/json"));
    }
}
