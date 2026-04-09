namespace BackendDistribuidores.Models;

/// <summary>Configuración global de envío: tarifa en USD por libra de peso total del pedido.</summary>
public class EnvioConfig
{
    public const int SingletonId = 1;

    public int Id { get; set; } = SingletonId;

    /// <summary>Precio de envío por libra (USD). 0 = sin cargo por peso.</summary>
    public decimal UsdPerLb { get; set; }
}
