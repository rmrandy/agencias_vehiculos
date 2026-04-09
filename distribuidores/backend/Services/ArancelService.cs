using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Aranceles por país de destino (solo LATAM), configurables desde el distribuidor.</summary>
public class ArancelService
{
    private readonly AppDbContext _db;

    public ArancelService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<decimal> GetTariffPercentAsync(string countryCode, CancellationToken ct = default)
    {
        var code = LatamCountries.Normalize(countryCode);
        if (!LatamCountries.IsValidCode(code))
            return 0;
        var row = await _db.ArancelPaises.AsNoTracking().FirstOrDefaultAsync(a => a.CountryCode == code, ct);
        return row?.TariffPercent ?? 0;
    }

    public async Task<List<ArancelPais>> ListAsync(CancellationToken ct = default)
    {
        return await _db.ArancelPaises.AsNoTracking().OrderBy(a => a.CountryName).ToListAsync(ct);
    }

    public async Task<ArancelPais?> UpdateAsync(string countryCode, decimal tariffPercent, CancellationToken ct = default)
    {
        var code = LatamCountries.Normalize(countryCode);
        if (!LatamCountries.IsValidCode(code))
            throw new ArgumentException("Código de país no válido o fuera de LATAM");
        if (tariffPercent < 0 || tariffPercent > 100)
            throw new ArgumentException("El arancel debe estar entre 0 y 100");

        var row = await _db.ArancelPaises.FirstOrDefaultAsync(a => a.CountryCode == code, ct);
        if (row == null)
        {
            row = new ArancelPais
            {
                CountryCode = code,
                CountryName = LatamCountries.All[code],
                TariffPercent = tariffPercent
            };
            _db.ArancelPaises.Add(row);
        }
        else
        {
            row.TariffPercent = tariffPercent;
        }

        await _db.SaveChangesAsync(ct);
        return row;
    }
}
