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
    /// <summary>Arancel sobre líneas importadas (fábrica), según país de destino LATAM.</summary>
    public decimal TariffTotal { get; set; }
    public decimal Total { get; set; }
    public string Currency { get; set; } = "USD";
    /// <summary>País de destino del envío (ISO2 LATAM) cuando aplica arancel.</summary>
    public string? ShippingCountryCode { get; set; }
    public DateTime? CreatedAt { get; set; }
}
