namespace BackendDistribuidores.Models;

public class CreatePedidoRequest
{
    public long? UserId { get; set; }
    public List<PedidoItemRequest>? Items { get; set; }
    public PaymentRequest? Payment { get; set; }
}

public class PaymentRequest
{
    public string? CardNumber { get; set; }
    public int? ExpiryMonth { get; set; }
    public int? ExpiryYear { get; set; }
}

public class PedidoItemRequest
{
    /// <summary>local (catálogo distribuidora) o fabrica.</summary>
    public string? Source { get; set; }
    public long? PartId { get; set; }
    public long? ProveedorId { get; set; }
    public long? FabricaPartId { get; set; }
    public int Qty { get; set; }
    /// <summary>Obligatorio para líneas de fábrica (precio mostrado en catálogo unificado).</summary>
    public decimal? UnitPrice { get; set; }
    public string? Title { get; set; }
    public string? PartNumber { get; set; }

    public static bool IsFabricLine(PedidoItemRequest i) =>
        string.Equals(i.Source, "fabrica", StringComparison.OrdinalIgnoreCase)
        || (i.ProveedorId.HasValue && i.FabricaPartId.HasValue);
}

public class UpdateEstadoRequest
{
    public long? UserId { get; set; }
    public string? Status { get; set; }
    public string? Comment { get; set; }
    public string? TrackingNumber { get; set; }
    public int? EtaDays { get; set; }
}
