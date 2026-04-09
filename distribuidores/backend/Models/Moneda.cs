namespace BackendDistribuidores.Models;

/// <summary>Divisa de cobro. Los precios de catálogo se mantienen en USD; UnitsPerUsd convierte a esta moneda.</summary>
public class Moneda
{
    /// <summary>ISO 4217 (3 letras).</summary>
    public string Code { get; set; } = "";

    public string Name { get; set; } = "";

    /// <summary>Símbolo para mostrar (ej. Q, MX$, €).</summary>
    public string Symbol { get; set; } = "";

    /// <summary>Cuántas unidades de esta moneda equivalen a 1 USD (ej. GTQ ≈ 7.85).</summary>
    public decimal UnitsPerUsd { get; set; } = 1;

    public bool Activo { get; set; } = true;

    public int SortOrder { get; set; }
}
