using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Pedidos locales y multi-fábrica (orden maestro en distribuidora + POST /api/pedidos por proveedor).</summary>
public class OrderService
{
    private readonly AppDbContext _db;
    private readonly PartService _partService;
    private readonly FabricaIntegrationService _fabrica;

    public OrderService(AppDbContext db, PartService partService, FabricaIntegrationService fabrica)
    {
        _db = db;
        _partService = partService;
        _fabrica = fabrica;
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

    /// <summary>Catálogo local + líneas de N fábricas: primero crea pedidos en cada fábrica, luego orden maestro aquí.</summary>
    public async Task<OrderHeader> CreateMultiSourceOrderAsync(long userId, List<PedidoItemRequest> items, CancellationToken ct = default)
    {
        if (items == null || items.Count == 0)
            throw new ArgumentException("El pedido debe tener al menos un artículo");

        var localLines = items.Where(i => !PedidoItemRequest.IsFabricLine(i)).ToList();
        var fabricLines = items.Where(PedidoItemRequest.IsFabricLine).ToList();

        foreach (var i in localLines)
        {
            if (!i.PartId.HasValue || i.PartId.Value <= 0)
                throw new ArgumentException("Cada línea local debe incluir partId");
            if (i.Qty <= 0)
                throw new ArgumentException("Cantidad inválida");
        }

        foreach (var i in fabricLines)
        {
            if (!i.ProveedorId.HasValue || !i.FabricaPartId.HasValue)
                throw new ArgumentException("Línea de fábrica: proveedorId y fabricaPartId son obligatorios");
            if (i.Qty <= 0)
                throw new ArgumentException("Cantidad inválida");
            if (!i.UnitPrice.HasValue || i.UnitPrice.Value < 0)
                throw new ArgumentException("Cada línea de fábrica requiere unitPrice");
        }

        var fabricByProv = fabricLines.GroupBy(x => x.ProveedorId!.Value).ToList();
        var fabricaOrderIds = new Dictionary<long, long>();

        foreach (var g in fabricByProv)
        {
            var prov = await _db.Proveedores.AsNoTracking()
                .FirstOrDefaultAsync(p => p.ProveedorId == g.Key, ct)
                ?? throw new ArgumentException($"Proveedor no encontrado: {g.Key}");
            if (string.IsNullOrWhiteSpace(prov.ApiBaseUrl))
                throw new ArgumentException($"El proveedor «{prov.Nombre}» no tiene apiBaseUrl");
            if (!prov.FabricaEnterpriseUserId.HasValue)
                throw new ArgumentException(
                    $"Configure fabricaEnterpriseUserId (usuario en la fábrica) para el proveedor «{prov.Nombre}»");

            var merged = g
                .GroupBy(x => x.FabricaPartId!.Value)
                .Select(gr => (PartId: gr.Key, Qty: gr.Sum(x => x.Qty)))
                .ToList();

            var oid = await _fabrica.CreatePedidoAsync(
                prov.ApiBaseUrl,
                prov.FabricaEnterpriseUserId.Value,
                merged,
                ct);
            fabricaOrderIds[g.Key] = oid;
        }

        foreach (var item in localLines)
        {
            var part = await _db.Parts.FindAsync(new object[] { item.PartId!.Value }, ct);
            if (part == null)
                throw new ArgumentException($"Repuesto no encontrado: {item.PartId}");
            if (!_partService.CheckAvailability(item.PartId.Value, item.Qty))
                throw new ArgumentException($"Stock insuficiente para: {part.Title}");
        }

        foreach (var item in localLines)
        {
            if (!_partService.ReserveStock(item.PartId!.Value, item.Qty))
            {
                RollbackReservations(localLines.Select(i => new OrderItemDto { PartId = i.PartId!.Value, Qty = i.Qty }).ToList());
                throw new InvalidOperationException("No se pudo reservar el stock local");
            }
        }

        decimal subtotal = 0;
        foreach (var item in localLines)
        {
            var part = (await _db.Parts.FindAsync(new object[] { item.PartId!.Value }, ct))!;
            subtotal += part.Price * item.Qty;
        }

        foreach (var item in fabricLines)
            subtotal += item.UnitPrice!.Value * item.Qty;

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

        foreach (var item in localLines)
        {
            var part = (await _db.Parts.FindAsync(new object[] { item.PartId!.Value }, ct))!;
            decimal lineTotal = part.Price * item.Qty;
            _db.OrderItems.Add(new OrderItem
            {
                OrderId = header.OrderId,
                LineSource = "LOCAL",
                PartId = item.PartId,
                Qty = item.Qty,
                UnitPrice = part.Price,
                LineTotal = lineTotal
            });
            _partService.ConfirmSale(item.PartId.Value, item.Qty);
        }

        foreach (var item in fabricLines)
        {
            long fabricOid = fabricaOrderIds[item.ProveedorId!.Value];
            _db.OrderItems.Add(new OrderItem
            {
                OrderId = header.OrderId,
                LineSource = "FABRICA",
                PartId = null,
                ProveedorId = item.ProveedorId,
                FabricaPartId = item.FabricaPartId,
                FabricaOrderId = fabricOid,
                Qty = item.Qty,
                UnitPrice = item.UnitPrice!.Value,
                LineTotal = item.UnitPrice.Value * item.Qty,
                TitleSnapshot = item.Title,
                PartNumberSnapshot = item.PartNumber
            });
        }

        await _db.SaveChangesAsync(ct);

        _db.OrderStatusHistories.Add(new OrderStatusHistory
        {
            OrderId = header.OrderId,
            Status = "INITIATED",
            CommentText = fabricLines.Count > 0
                ? "Pedido creado (incluye líneas en fábrica)"
                : "Pedido creado",
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
