namespace BackendDistribuidores.Models;

/// <summary>Catálogo fijo de países de América Latina y el Caribe (ISO 3166-1 alpha-2) para aranceles.</summary>
public static class LatamCountries
{
    /// <summary>Código ISO → nombre en español.</summary>
    public static readonly IReadOnlyDictionary<string, string> All = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
    {
        ["MX"] = "México",
        ["GT"] = "Guatemala",
        ["BZ"] = "Belice",
        ["SV"] = "El Salvador",
        ["HN"] = "Honduras",
        ["NI"] = "Nicaragua",
        ["CR"] = "Costa Rica",
        ["PA"] = "Panamá",
        ["CU"] = "Cuba",
        ["DO"] = "República Dominicana",
        ["HT"] = "Haití",
        ["JM"] = "Jamaica",
        ["TT"] = "Trinidad y Tobago",
        ["BS"] = "Bahamas",
        ["BB"] = "Barbados",
        ["AG"] = "Antigua y Barbuda",
        ["DM"] = "Dominica",
        ["GD"] = "Granada",
        ["KN"] = "San Cristóbal y Nieves",
        ["LC"] = "Santa Lucía",
        ["VC"] = "San Vicente y las Granadinas",
        ["AR"] = "Argentina",
        ["BO"] = "Bolivia",
        ["BR"] = "Brasil",
        ["CL"] = "Chile",
        ["CO"] = "Colombia",
        ["EC"] = "Ecuador",
        ["GY"] = "Guyana",
        ["PY"] = "Paraguay",
        ["PE"] = "Perú",
        ["SR"] = "Surinam",
        ["UY"] = "Uruguay",
        ["VE"] = "Venezuela",
    };

    public static bool IsValidCode(string? code) =>
        !string.IsNullOrWhiteSpace(code) && All.ContainsKey(code.Trim());

    public static string Normalize(string? code) => (code ?? "").Trim().ToUpperInvariant();
}
