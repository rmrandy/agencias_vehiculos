using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

public class MonedaService
{
    private readonly AppDbContext _db;

    public MonedaService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<List<Moneda>> ListActiveAsync(CancellationToken ct = default)
    {
        return await _db.Monedas.AsNoTracking()
            .Where(m => m.Activo)
            .OrderBy(m => m.SortOrder)
            .ThenBy(m => m.Name)
            .ToListAsync(ct);
    }

    /// <summary>Multiplicador para pasar de importes en USD a la divisa indicada. USD siempre 1.</summary>
    public async Task<(decimal Mult, string Code)> ResolveMultiplierAsync(string? currencyCode, CancellationToken ct = default)
    {
        var code = string.IsNullOrWhiteSpace(currencyCode) ? "USD" : currencyCode.Trim().ToUpperInvariant();
        if (code == "USD")
            return (1m, "USD");

        var row = await _db.Monedas.AsNoTracking()
            .FirstOrDefaultAsync(m => m.Code == code && m.Activo, ct);
        if (row == null)
            throw new ArgumentException($"Divisa no disponible: {code}");
        if (row.UnitsPerUsd <= 0)
            throw new ArgumentException($"Tipo de cambio inválido para {code}");

        return (row.UnitsPerUsd, code);
    }

    public async Task<Moneda?> UpdateUnitsPerUsdAsync(string currencyCode, decimal unitsPerUsd, CancellationToken ct = default)
    {
        var code = (currencyCode ?? "").Trim().ToUpperInvariant();
        if (code.Length != 3)
            throw new ArgumentException("Código de divisa inválido");
        if (code == "USD" && unitsPerUsd != 1)
            throw new ArgumentException("USD debe mantener UnitsPerUsd = 1");
        if (unitsPerUsd <= 0)
            throw new ArgumentException("UnitsPerUsd debe ser mayor que 0");

        var row = await _db.Monedas.FirstOrDefaultAsync(m => m.Code == code, ct);
        if (row == null)
            throw new ArgumentException($"Divisa no encontrada: {code}");
        if (string.Equals(code, "USD", StringComparison.Ordinal))
        {
            row.UnitsPerUsd = 1;
        }
        else
        {
            row.UnitsPerUsd = unitsPerUsd;
        }

        await _db.SaveChangesAsync(ct);
        return row;
    }
}
