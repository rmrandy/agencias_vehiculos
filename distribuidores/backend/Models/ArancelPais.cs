namespace BackendDistribuidores.Models;

/// <summary>Arancel (%) aplicable a importaciones según país de destino en LATAM.</summary>
public class ArancelPais
{
    /// <summary>ISO 3166-1 alpha-2 (debe existir en <see cref="LatamCountries"/>).</summary>
    public string CountryCode { get; set; } = "";

    public string CountryName { get; set; } = "";

    /// <summary>Porcentaje de arancel sobre el subtotal importado (0–100).</summary>
    public decimal TariffPercent { get; set; }
}
