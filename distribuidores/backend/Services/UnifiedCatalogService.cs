using System.Text.Json;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Catálogo local + N fábricas (proveedores con ApiBaseUrl activo).</summary>
public sealed class UnifiedCatalogService
{
    private readonly AppDbContext _db;
    private readonly PartService _partService;
    private readonly FabricaIntegrationService _fabrica;

    public UnifiedCatalogService(AppDbContext db, PartService partService, FabricaIntegrationService fabrica)
    {
        _db = db;
        _partService = partService;
        _fabrica = fabrica;
    }

    public async Task<List<object>> SearchAsync(string? term, CancellationToken ct)
    {
        var t = term?.Trim() ?? "";
        var results = new List<object>();

        var local = await _partService.SearchAsync(string.IsNullOrEmpty(t) ? null : t, ct);
        foreach (var p in local)
            results.Add(ToLocalRow(p));

        var proveedores = await _db.Proveedores
            .AsNoTracking()
            .Where(p => p.Activo && p.ApiBaseUrl != null && p.ApiBaseUrl != "")
            .OrderBy(p => p.Nombre)
            .ToListAsync(ct);

        foreach (var prov in proveedores)
        {
            try
            {
                var json = await _fabrica.GetBusquedaRepuestosJsonAsync(prov.ApiBaseUrl!, t, ct);
                using var doc = JsonDocument.Parse(json);
                var root = doc.RootElement;
                if (root.ValueKind != JsonValueKind.Array)
                    continue;
                foreach (var el in root.EnumerateArray())
                {
                    var row = MapFabricRow(el, prov);
                    if (row != null)
                        results.Add(row);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[UnifiedCatalog] proveedor {prov.ProveedorId} ({prov.Nombre}): {ex.Message}");
            }
        }

        return results;
    }

    private static object ToLocalRow(Part p)
    {
        return new
        {
            source = "local",
            proveedorId = (long?)null,
            proveedorNombre = (string?)null,
            fabricaBaseUrl = (string?)null,
            partId = p.PartId,
            categoryId = p.CategoryId,
            brandId = p.BrandId,
            partNumber = p.PartNumber,
            title = p.Title,
            description = p.Description,
            compatibilityTags = p.CompatibilityTags,
            weightLb = p.WeightLb,
            price = p.Price,
            active = p.Active,
            createdAt = p.CreatedAt,
            hasImage = p.HasImage,
            inStock = p.InStock,
            lowStock = p.LowStock,
            stockQuantity = p.StockQuantity,
            availableQuantity = p.AvailableQuantity
        };
    }

    private static object? MapFabricRow(JsonElement el, Proveedor prov)
    {
        if (!TryGetInt64(el, "partId", out var partId))
            return null;

        var baseUrl = prov.ApiBaseUrl!.Trim().TrimEnd('/');
        decimal price = 0;
        if (el.TryGetProperty("price", out var pr))
        {
            if (pr.ValueKind == JsonValueKind.Number)
                price = pr.GetDecimal();
        }

        bool inStock = true;
        if (el.TryGetProperty("inStock", out var st) && st.ValueKind == JsonValueKind.False)
            inStock = false;

        bool hasImage = el.TryGetProperty("hasImage", out var hi) && hi.ValueKind == JsonValueKind.True;

        var title = el.TryGetProperty("title", out var tt) ? tt.GetString() ?? "" : "";
        var partNumber = el.TryGetProperty("partNumber", out var pn) ? pn.GetString() ?? "" : "";
        int active = 1;
        if (el.TryGetProperty("active", out var ac) && ac.ValueKind == JsonValueKind.Number)
            active = ac.GetInt32();

        return new
        {
            source = "fabrica",
            proveedorId = prov.ProveedorId,
            proveedorNombre = prov.Nombre,
            fabricaBaseUrl = baseUrl,
            partId,
            categoryId = TryGetInt64Nullable(el, "categoryId"),
            brandId = TryGetInt64Nullable(el, "brandId"),
            partNumber,
            title,
            description = el.TryGetProperty("description", out var d) ? d.GetString() : null,
            compatibilityTags = el.TryGetProperty("compatibilityTags", out var tags) ? tags.GetString() : null,
            weightLb = TryGetDecimalNullable(el, "weightLb"),
            price,
            active,
            createdAt = (string?)null,
            hasImage,
            inStock,
            lowStock = el.TryGetProperty("lowStock", out var ls) && ls.ValueKind == JsonValueKind.True,
            stockQuantity = TryGetInt32Nullable(el, "stockQuantity"),
            availableQuantity = TryGetInt32Nullable(el, "availableQuantity")
        };
    }

    private static bool TryGetInt64(JsonElement el, string name, out long v)
    {
        v = 0;
        if (!el.TryGetProperty(name, out var p))
            return false;
        if (p.ValueKind == JsonValueKind.Number)
        {
            v = p.GetInt64();
            return true;
        }
        return false;
    }

    private static long? TryGetInt64Nullable(JsonElement el, string name)
    {
        return TryGetInt64(el, name, out var v) ? v : null;
    }

    private static int? TryGetInt32Nullable(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind != JsonValueKind.Number)
            return null;
        return p.GetInt32();
    }

    private static decimal? TryGetDecimalNullable(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind != JsonValueKind.Number)
            return null;
        return p.GetDecimal();
    }
}
