namespace BackendDistribuidores.Models;

/// <summary>Línea de pedido (misma lógica que fábrica OrderItem).</summary>
public class OrderItem
{
    public long OrderItemId { get; set; }
    public long OrderId { get; set; }
    public long PartId { get; set; }
    public int Qty { get; set; }
    public decimal UnitPrice { get; set; }
    public decimal LineTotal { get; set; }
}
