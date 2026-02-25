using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Pedidos locales (misma lógica que fábrica: una única orden, estado INITIATED).</summary>
public class OrderService
{
    private readonly AppDbContext _db;
    private readonly PartService _partService;

    public OrderService(AppDbContext db, PartService partService)
    {
        _db = db;
        _partService = partService;
    }

    public async Task<OrderHeader> CreateOrderAsync(long userId, List<OrderItemDto> items, CancellationToken ct = default)
    {
        if (items == null || items.Count == 0)
            throw new ArgumentException("El pedido debe tener al menos un artículo");

        foreach (var item in items)
        {
            var part = await _db.Parts.FindAsync(new object[] { item.PartId }, ct);
            if (part == null)
                throw new ArgumentException($"Repuesto no encontrado: {item.PartId}");
            if (!_partService.CheckAvailability(item.PartId, item.Qty))
                throw new ArgumentException($"Stock insuficiente para: {part.Title}");
        }

        foreach (var item in items)
        {
            if (!_partService.ReserveStock(item.PartId, item.Qty))
            {
                RollbackReservations(items);
                throw new InvalidOperationException("No se pudo reservar el stock");
            }
        }

        decimal subtotal = 0;
        foreach (var item in items)
        {
            var part = (await _db.Parts.FindAsync(new object[] { item.PartId }, ct))!;
            subtotal += part.Price * item.Qty;
        }

        decimal shippingTotal = 0;
        decimal total = subtotal + shippingTotal;
        string orderNumber = GenerateOrderNumber();

        var header = new OrderHeader
        {
            OrderNumber = orderNumber,
            UserId = userId,
            OrderType = "WEB",
            Subtotal = subtotal,
            ShippingTotal = shippingTotal,
            Total = total,
            Currency = "USD",
            CreatedAt = DateTime.UtcNow
        };
        _db.OrderHeaders.Add(header);
        await _db.SaveChangesAsync(ct);

        foreach (var item in items)
        {
            var part = (await _db.Parts.FindAsync(new object[] { item.PartId }, ct))!;
            decimal lineTotal = part.Price * item.Qty;
            _db.OrderItems.Add(new OrderItem
            {
                OrderId = header.OrderId,
                PartId = item.PartId,
                Qty = item.Qty,
                UnitPrice = part.Price,
                LineTotal = lineTotal
            });
            _partService.ConfirmSale(item.PartId, item.Qty);
        }
        await _db.SaveChangesAsync(ct);

        _db.OrderStatusHistories.Add(new OrderStatusHistory
        {
            OrderId = header.OrderId,
            Status = "INITIATED",
            CommentText = "Pedido creado",
            ChangedByUserId = userId,
            ChangedAt = DateTime.UtcNow
        });
        await _db.SaveChangesAsync(ct);

        return header;
    }

    public async Task<List<OrderHeader>> GetByUserIdAsync(long userId, CancellationToken ct = default)
    {
        return await _db.OrderHeaders
            .Where(o => o.UserId == userId)
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync(ct);
    }

    public async Task<OrderHeader?> GetByIdAsync(long orderId, CancellationToken ct = default)
    {
        return await _db.OrderHeaders.FindAsync(new object[] { orderId }, ct);
    }

    public async Task<List<OrderItem>> GetItemsAsync(long orderId, CancellationToken ct = default)
    {
        return await _db.OrderItems.Where(i => i.OrderId == orderId).ToListAsync(ct);
    }

    public async Task<OrderStatusHistory?> GetLatestStatusAsync(long orderId, CancellationToken ct = default)
    {
        return await _db.OrderStatusHistories
            .Where(s => s.OrderId == orderId)
            .OrderByDescending(s => s.ChangedAt)
            .FirstOrDefaultAsync(ct);
    }

    /// <summary>Todos los pedidos (para admin).</summary>
    public async Task<List<OrderHeader>> GetAllOrdersAsync(CancellationToken ct = default)
    {
        return await _db.OrderHeaders
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync(ct);
    }

    /// <summary>Añade un nuevo estado al pedido (para admin/empleado).</summary>
    public async Task<OrderStatusHistory> AddOrderStatusAsync(long orderId, string status, string? comment, string? trackingNumber, int? etaDays, long changedByUserId, CancellationToken ct = default)
    {
        var order = await GetByIdAsync(orderId, ct);
        if (order == null)
            throw new ArgumentException("Pedido no encontrado");
        var entry = new OrderStatusHistory
        {
            OrderId = orderId,
            Status = status?.Trim() ?? "INITIATED",
            CommentText = comment,
            TrackingNumber = trackingNumber,
            EtaDays = etaDays,
            ChangedByUserId = changedByUserId,
            ChangedAt = DateTime.UtcNow
        };
        _db.OrderStatusHistories.Add(entry);
        await _db.SaveChangesAsync(ct);
        return entry;
    }

    private static string GenerateOrderNumber()
    {
        return "ORD-" + DateTime.UtcNow.ToString("yyyyMMdd-HHmmss") + "-" + Guid.NewGuid().ToString("N")[..8];
    }

    private void RollbackReservations(List<OrderItemDto> items)
    {
        foreach (var item in items)
        {
            try { _partService.ReleaseStock(item.PartId, item.Qty); }
            catch { /* ignore */ }
        }
    }
}

public class OrderItemDto
{
    public long PartId { get; set; }
    public int Qty { get; set; }
}
