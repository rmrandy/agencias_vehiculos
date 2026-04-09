using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

public class ShippingRateService
{
    private readonly AppDbContext _db;

    public ShippingRateService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<decimal> GetUsdPerLbAsync(CancellationToken ct = default)
    {
        var row = await _db.EnvioConfigs.AsNoTracking()
            .FirstOrDefaultAsync(e => e.Id == EnvioConfig.SingletonId, ct);
        return row?.UsdPerLb ?? 0;
    }

    public async Task<EnvioConfig> SetUsdPerLbAsync(decimal usdPerLb, CancellationToken ct = default)
    {
        if (usdPerLb < 0)
            throw new ArgumentException("La tarifa por libra no puede ser negativa");

        var row = await _db.EnvioConfigs.FirstOrDefaultAsync(e => e.Id == EnvioConfig.SingletonId, ct);
        if (row == null)
        {
            row = new EnvioConfig { Id = EnvioConfig.SingletonId, UsdPerLb = usdPerLb };
            _db.EnvioConfigs.Add(row);
        }
        else
        {
            row.UsdPerLb = usdPerLb;
        }

        await _db.SaveChangesAsync(ct);
        return row;
    }
}
