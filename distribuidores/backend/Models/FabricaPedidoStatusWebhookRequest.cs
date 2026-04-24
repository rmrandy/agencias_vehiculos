namespace BackendDistribuidores.Models;

/// <summary>Cuerpo del POST desde la fábrica para alinear el estado del pedido maestro en la distribuidora.</summary>
public sealed class FabricaPedidoStatusWebhookRequest
{
    public long FabricaOrderId { get; set; }
    public long? ProveedorId { get; set; }
    public string Status { get; set; } = "";
    public string? Comment { get; set; }
    public string? TrackingNumber { get; set; }
    public int? EtaDays { get; set; }
}
