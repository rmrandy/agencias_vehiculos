using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Reportería comercial sobre pedidos e ítems (usa tablas existentes; no requiere DDL).</summary>
public class ReportesService
{
    private readonly AppDbContext _db;

    public ReportesService(AppDbContext db)
    {
        _db = db;
    }

    private static DateTime DefaultFrom() => DateTime.UtcNow.Date.AddMonths(-3);
    private static DateTime DefaultToEnd() => DateTime.UtcNow.Date.AddDays(1).AddTicks(-1);

    private static Dictionary<long, string> LatestStatusMap(IEnumerable<OrderStatusHistory> rows)
    {
        return rows
            .GroupBy(s => s.OrderId)
            .ToDictionary(
                g => g.Key,
                g => g.OrderByDescending(x => x.ChangedAt ?? DateTime.MinValue).First().Status ?? "INITIATED");
    }

    public async Task<List<MasVendidoRow>> GetMasVendidosAsync(DateTime? from, DateTime? to, int top, CancellationToken ct = default)
    {
        var fromDate = from ?? DefaultFrom();
        var toEnd = to?.Date.AddDays(1).AddTicks(-1) ?? DefaultToEnd();
        if (top <= 0) top = 30;

        var orderIds = await _db.OrderHeaders.AsNoTracking()
            .Where(h => h.CreatedAt != null && h.CreatedAt >= fromDate && h.CreatedAt <= toEnd)
            .Select(h => h.OrderId)
            .ToListAsync(ct);
        if (orderIds.Count == 0)
            return new List<MasVendidoRow>();

        var statuses = await _db.OrderStatusHistories.AsNoTracking()
            .Where(s => orderIds.Contains(s.OrderId))
            .ToListAsync(ct);
        var latest = LatestStatusMap(statuses);
        var active = orderIds.Where(oid => !string.Equals(latest.GetValueOrDefault(oid), "CANCELLED", StringComparison.OrdinalIgnoreCase)).ToHashSet();

        var items = await _db.OrderItems.AsNoTracking()
            .Where(i => active.Contains(i.OrderId) && i.PartId != null && i.LineSource == "LOCAL")
            .ToListAsync(ct);

        var agg = items
            .GroupBy(i => i.PartId!.Value)
            .Select(g => new { PartId = g.Key, Qty = g.Sum(x => x.Qty), Total = g.Sum(x => x.LineTotal) })
            .OrderByDescending(x => x.Qty)
            .Take(top)
            .ToList();

        var partIds = agg.Select(x => x.PartId).ToList();
        var parts = await _db.Parts.AsNoTracking()
            .Where(p => partIds.Contains(p.PartId))
            .ToDictionaryAsync(p => p.PartId, p => p, ct);

        return agg.Select(x =>
        {
            parts.TryGetValue(x.PartId, out var p);
            return new MasVendidoRow(x.PartId, p?.PartNumber, p?.Title, x.Qty, x.Total);
        }).ToList();
    }

    public async Task<List<VentaDiariaRow>> GetVentasDiariasAsync(DateTime? from, DateTime? to, CancellationToken ct = default)
    {
        var fromDate = from ?? DefaultFrom();
        var toEnd = to?.Date.AddDays(1).AddTicks(-1) ?? DefaultToEnd();

        var headers = await _db.OrderHeaders.AsNoTracking()
            .Where(h => h.CreatedAt != null && h.CreatedAt >= fromDate && h.CreatedAt <= toEnd)
            .ToListAsync(ct);
        if (headers.Count == 0)
            return new List<VentaDiariaRow>();

        var ids = headers.Select(h => h.OrderId).ToList();
        var statuses = await _db.OrderStatusHistories.AsNoTracking()
            .Where(s => ids.Contains(s.OrderId))
            .ToListAsync(ct);
        var latest = LatestStatusMap(statuses);

        var rows = headers
            .Where(h => !string.Equals(latest.GetValueOrDefault(h.OrderId), "CANCELLED", StringComparison.OrdinalIgnoreCase))
            .GroupBy(h => h.CreatedAt!.Value.Date)
            .Select(g => new VentaDiariaRow(g.Key, g.Count(), g.Sum(x => x.Total)))
            .OrderBy(x => x.Fecha)
            .ToList();

        return rows;
    }

    public async Task<List<PedidoEstadoRow>> GetPedidosPorEstadoAsync(DateTime? from, DateTime? to, CancellationToken ct = default)
    {
        var fromDate = from ?? DefaultFrom();
        var toEnd = to?.Date.AddDays(1).AddTicks(-1) ?? DefaultToEnd();

        var headers = await _db.OrderHeaders.AsNoTracking()
            .Where(h => h.CreatedAt != null && h.CreatedAt >= fromDate && h.CreatedAt <= toEnd)
            .ToListAsync(ct);
        if (headers.Count == 0)
            return new List<PedidoEstadoRow>();

        var ids = headers.Select(h => h.OrderId).ToList();
        var statuses = await _db.OrderStatusHistories.AsNoTracking()
            .Where(s => ids.Contains(s.OrderId))
            .ToListAsync(ct);
        var latest = LatestStatusMap(statuses);

        return headers
            .Select(h => new { h.Total, Estado = latest.GetValueOrDefault(h.OrderId, "INITIATED") })
            .GroupBy(x => x.Estado)
            .Select(g => new PedidoEstadoRow(g.Key, g.Count(), g.Sum(x => x.Total)))
            .OrderByDescending(x => x.CantidadPedidos)
            .ToList();
    }
}

public sealed record MasVendidoRow(long PartId, string? PartNumber, string? PartTitle, int TotalQty, decimal TotalImporte);

public sealed record VentaDiariaRow(DateTime Fecha, int PedidoCount, decimal TotalImporte);

public sealed record PedidoEstadoRow(string Estado, int CantidadPedidos, decimal TotalImporte);
