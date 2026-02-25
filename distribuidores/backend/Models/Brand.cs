namespace BackendDistribuidores.Models;

/// <summary>Marca de repuestos (misma lógica que fábrica).</summary>
public class Brand
{
    public long BrandId { get; set; }
    public string Name { get; set; } = string.Empty;
    public byte[]? ImageData { get; set; }
    public string? ImageType { get; set; }
}
