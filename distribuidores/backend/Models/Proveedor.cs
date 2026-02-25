namespace BackendDistribuidores.Models;

/// <summary>Proveedor (DOC2: agregar/eliminar dinámicamente; internacional con tipo de cambio).</summary>
public class Proveedor
{
    public long ProveedorId { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public string? Contacto { get; set; }
    public string? Email { get; set; }
    public string? Telefono { get; set; }
    /// <summary>Base URL del API REST del proveedor (ej. fábrica).</summary>
    public string? ApiBaseUrl { get; set; }
    /// <summary>Si es internacional: tipo de cambio a quetzales (GTQ).</summary>
    public decimal? TipoCambioAQuetzales { get; set; }
    /// <summary>Si es internacional: porcentaje de ganancia configurable.</summary>
    public decimal? PorcentajeGanancia { get; set; }
    /// <summary>Costo de envío por libra (peso) para internacionales.</summary>
    public decimal? CostoEnvioPorLibra { get; set; }
    public bool Activo { get; set; } = true;
}
