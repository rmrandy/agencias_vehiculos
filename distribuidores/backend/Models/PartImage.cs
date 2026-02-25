using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace BackendDistribuidores.Models;

/// <summary>Imagen adicional del repuesto (galería: varias fotos por producto).</summary>
public class PartImage
{
    public long PartImageId { get; set; }
    public long PartId { get; set; }
    public int SortOrder { get; set; }

    [JsonIgnore]
    public byte[] ImageData { get; set; } = Array.Empty<byte>();
    public string? ImageType { get; set; }
}
