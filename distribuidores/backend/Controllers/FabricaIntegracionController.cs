using BackendDistribuidores.Models;
using BackendDistribuidores.Services;
using Microsoft.AspNetCore.Mvc;

namespace BackendDistribuidores.Controllers;

/// <summary>Callbacks desde el backend de la fábrica (sincronizar estado del pedido maestro).</summary>
[ApiController]
[Route("api/integracion/fabrica")]
public class FabricaIntegracionController : ControllerBase
{
    private readonly OrderService _orderService;
    private readonly IConfiguration _config;

    public FabricaIntegracionController(OrderService orderService, IConfiguration config)
    {
        _orderService = orderService;
        _config = config;
    }

    /// <summary>
    /// La fábrica llama este endpoint tras cambiar el estado de un pedido con origen distribuidora.
    /// Requiere cabecera <c>X-Fabrica-Webhook-Secret</c> igual a <c>Integration:FabricaWebhookSecret</c>.
    /// </summary>
    [HttpPost("sync-estado-pedido")]
    public async Task<IActionResult> SyncEstadoPedido([FromBody] FabricaPedidoStatusWebhookRequest? body, CancellationToken ct)
    {
        if (body == null || body.FabricaOrderId <= 0 || string.IsNullOrWhiteSpace(body.Status))
            return BadRequest(new { message = "fabricaOrderId y status son obligatorios" });

        var expected = _config["Integration:FabricaWebhookSecret"]?.Trim();
        if (string.IsNullOrEmpty(expected))
            return NotFound(new { message = "Webhook no habilitado (configure Integration:FabricaWebhookSecret)." });

        var header = Request.Headers["X-Fabrica-Webhook-Secret"].FirstOrDefault()?.Trim();
        if (!string.Equals(header, expected, StringComparison.Ordinal))
            return Unauthorized();

        try
        {
            var updated = await _orderService.ApplyFabricaPedidoStatusNotifyAsync(
                body.FabricaOrderId,
                body.ProveedorId,
                body.Status,
                body.Comment,
                body.TrackingNumber,
                body.EtaDays,
                ct);
            return Ok(new { updatedOrderIds = updated, count = updated.Count });
        }
        catch (ArgumentException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
