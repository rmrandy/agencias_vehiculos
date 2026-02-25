using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace BackendDistribuidores.Models;

/// <summary>Repuesto del catálogo local (misma lógica que fábrica Part).</summary>
public class Part
{
    public long PartId { get; set; }
    public long CategoryId { get; set; }
    public long BrandId { get; set; }
    public string PartNumber { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public decimal? WeightLb { get; set; }
    public decimal Price { get; set; }
    public int Active { get; set; } = 1;
    public DateTime? CreatedAt { get; set; }

    [JsonIgnore]
    public byte[]? ImageData { get; set; }
    public string? ImageType { get; set; }

    public int StockQuantity { get; set; }
    public int LowStockThreshold { get; set; } = 5;
    public int ReservedQuantity { get; set; }

    [NotMapped]
    public bool HasImage => ImageData != null && ImageData.Length > 0;

    [NotMapped]
    public int AvailableQuantity => StockQuantity - ReservedQuantity;

    [NotMapped]
    public bool InStock => AvailableQuantity > 0;

    [NotMapped]
    public bool LowStock => AvailableQuantity > 0 && AvailableQuantity <= LowStockThreshold;
}
