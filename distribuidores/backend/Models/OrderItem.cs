namespace BackendDistribuidores.Models;

/// <summary>Línea de pedido local o remota (fábrica vía API).</summary>
public class OrderItem
{
    public long OrderItemId { get; set; }
    public long OrderId { get; set; }
    /// <summary>LOCAL o FABRICA.</summary>
    public string LineSource { get; set; } = "LOCAL";
    /// <summary>Repuesto del catálogo local; null si la línea es solo de fábrica.</summary>
    public long? PartId { get; set; }
    public int Qty { get; set; }
    public decimal UnitPrice { get; set; }
    public decimal LineTotal { get; set; }
    public long? ProveedorId { get; set; }
    public long? FabricaPartId { get; set; }
    /// <summary>ORDER_ID del pedido creado en la fábrica (mismo valor para todas las líneas de ese proveedor en este checkout).</summary>
    public long? FabricaOrderId { get; set; }
    public string? TitleSnapshot { get; set; }
    public string? PartNumberSnapshot { get; set; }
}
