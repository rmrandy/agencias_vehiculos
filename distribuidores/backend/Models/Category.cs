namespace BackendDistribuidores.Models;

/// <summary>Categoría de repuestos (misma lógica que fábrica).</summary>
public class Category
{
    public long CategoryId { get; set; }
    public string Name { get; set; } = string.Empty;
    public long? ParentId { get; set; }
    public byte[]? ImageData { get; set; }
    public string? ImageType { get; set; }
}
