namespace BackendDistribuidores.Models;

/// <summary>Comentario o valoración sobre un repuesto (misma lógica que fábrica PartReview).</summary>
public class PartReview
{
    public long ReviewId { get; set; }
    public long PartId { get; set; }
    public long UserId { get; set; }
    public long? ParentId { get; set; }
    /// <summary>Puntuación 1-5 estrellas. Solo en comentarios raíz.</summary>
    public int? Rating { get; set; }
    public string Body { get; set; } = string.Empty;
    public DateTime? CreatedAt { get; set; }
}
