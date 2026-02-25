namespace BackendDistribuidores.Models;

/// <summary>Cabecera de pedido (misma lógica que fábrica OrderHeader).</summary>
public class OrderHeader
{
    public long OrderId { get; set; }
    public string OrderNumber { get; set; } = string.Empty;
    public long UserId { get; set; }
    public string OrderType { get; set; } = "WEB";
    public decimal Subtotal { get; set; }
    public decimal ShippingTotal { get; set; }
    public decimal Total { get; set; }
    public string Currency { get; set; } = "USD";
    public DateTime? CreatedAt { get; set; }
}
