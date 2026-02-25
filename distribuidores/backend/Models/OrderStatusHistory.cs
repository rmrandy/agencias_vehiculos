namespace BackendDistribuidores.Models;

/// <summary>Historial de estado del pedido (misma lógica que fábrica).</summary>
public class OrderStatusHistory
{
    public long StatusId { get; set; }
    public long OrderId { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? CommentText { get; set; }
    public string? TrackingNumber { get; set; }
    public int? EtaDays { get; set; }
    public long? ChangedByUserId { get; set; }
    public DateTime? ChangedAt { get; set; }
}
