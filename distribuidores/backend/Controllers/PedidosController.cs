using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// Pedidos de la distribuidora: creación local o multi-fuente (fábricas), listados, detalle con estado remoto,
/// recibo PDF y transiciones de estado (admin/empleado).
/// </summary>
[ApiController]
[Route("api/pedidos")]
public class PedidosController : ControllerBase
{
    private static readonly Regex OnlyDigits = new(@"^\d+$");

    private readonly OrderService _orderService;
    private readonly PartService _partService;
    private readonly MailService _mailService;
    private readonly PedidoReciboPdfService _reciboPdf;
    private readonly FabricaIntegrationService _fabricaIntegration;
    private readonly AppDbContext _db;

    public PedidosController(
        OrderService orderService,
        PartService partService,
        MailService mailService,
        PedidoReciboPdfService reciboPdf,
        FabricaIntegrationService fabricaIntegration,
        AppDbContext db)
    {
        _orderService = orderService;
        _partService = partService;
        _mailService = mailService;
        _reciboPdf = reciboPdf;
        _fabricaIntegration = fabricaIntegration;
        _db = db;
    }

    /// <summary>Crea un pedido; admite ítems solo locales o mezcla con líneas de fábrica según el cuerpo.</summary>
    /// <param name="request">Usuario, líneas (<c>partId</c> / identificadores de fábrica) y datos de pago opcionales.</param>
    /// <param name="ct">Token de cancelación.</param>
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreatePedidoRequest request, CancellationToken ct)
    {
        if (request?.UserId == null || request.Items == null || request.Items.Count == 0)
            return BadRequest(new { message = "userId e items son obligatorios" });

        if (request.Payment != null)
        {
            var cardError = ValidateCard(request.Payment);
            if (cardError != null)
                return BadRequest(new { message = cardError });
        }

        try
        {
            OrderHeader order;
            if (request.Items.Any(PedidoItemRequest.IsFabricLine))
            {
                order = await _orderService.CreateMultiSourceOrderAsync(
                    request.UserId.Value, request.Items, request.Payment, ct);
            }
            else
            {
                foreach (var i in request.Items)
                {
                    if (!i.PartId.HasValue || i.PartId.Value <= 0)
                        return BadRequest(new { message = "Cada ítem local debe incluir partId" });
                }

                var items = request.Items
                    .Select(i => new OrderItemDto { PartId = i.PartId!.Value, Qty = i.Qty })
                    .ToList();
                order = await _orderService.CreateOrderAsync(request.UserId.Value, items, ct);
            }

            if (request.Payment != null)
            {
                try
                {
                    var user = await _db.AppUsers.FindAsync(new object[] { order.UserId }, ct);
                    if (user != null && !string.IsNullOrWhiteSpace(user.Email))
                    {
                        var orderItems = await _orderService.GetItemsAsync(order.OrderId, ct);
                        var emailItems = new List<(string PartTitle, int Qty, decimal UnitPrice, decimal LineTotal)>();
                        foreach (var i in orderItems)
                        {
                            string title;
                            if (string.Equals(i.LineSource, "FABRICA", StringComparison.OrdinalIgnoreCase) || i.PartId == null)
                                title = i.TitleSnapshot ?? $"Repuesto fábrica #{i.FabricaPartId}";
                            else
                            {
                                var part = await _partService.GetByIdAsync(i.PartId.Value, ct);
                                title = part?.Title ?? "Repuesto #" + i.PartId;
                            }

                            emailItems.Add((title, i.Qty, i.UnitPrice, i.LineTotal));
                        }
                        _mailService.SendOrderConfirmation(user.Email, user.FullName, order, emailItems);
                    }
                }
                catch (Exception ex)
                {
                    // No fallar el pedido si el correo falla (igual que fábrica)
                    Console.WriteLine("Error enviando correo de confirmación: " + ex.Message);
                }
            }

            return StatusCode(201, new
            {
                orderId = order.OrderId,
                orderNumber = order.OrderNumber,
                userId = order.UserId,
                orderType = order.OrderType,
                subtotal = order.Subtotal,
                shippingTotal = order.ShippingTotal,
                total = order.Total,
                createdAt = order.CreatedAt
            });
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
        catch (InvalidOperationException e) { return BadRequest(new { message = e.Message }); }
    }

    /// <summary>Valida tarjeta (solo dígitos 13-19, vencimiento MM/YY en el futuro). Igual que fábrica.</summary>
    private static string? ValidateCard(PaymentRequest? payment)
    {
        if (payment == null) return null;
        var cardNumber = payment.CardNumber?.Trim() ?? "";
        var digits = Regex.Replace(cardNumber, @"\D", "");
        if (digits.Length < 13 || digits.Length > 19)
            return "El número de tarjeta debe tener entre 13 y 19 dígitos";
        if (!OnlyDigits.IsMatch(digits))
            return "El número de tarjeta solo puede contener dígitos";
        if (!payment.ExpiryMonth.HasValue || !payment.ExpiryYear.HasValue)
            return "La fecha de vencimiento (mes y año) es obligatoria";
        int month = payment.ExpiryMonth.Value;
        int year = payment.ExpiryYear.Value;
        if (year < 100) year += 2000;
        if (month < 1 || month > 12)
            return "El mes de vencimiento debe ser entre 01 y 12";
        var expiry = new DateTime(year, month, 1);
        if (expiry.Date < DateTime.Today)
            return "La tarjeta está vencida";
        return null;
    }

    /// <summary>Lista pedidos del usuario con resumen de estado local y enlaces a estados en fábrica cuando aplica.</summary>
    [HttpGet("usuario/{userId:long}")]
    public async Task<IActionResult> GetByUser(long userId, CancellationToken ct)
    {
        var list = await _orderService.GetByUserIdAsync(userId, ct);
        if (list.Count == 0)
            return Ok(Array.Empty<object>());

        var ids = list.Select(o => o.OrderId).ToList();
        var statusRows = await _db.OrderStatusHistories.AsNoTracking()
            .Where(s => ids.Contains(s.OrderId))
            .ToListAsync(ct);
        var latestByOrder = statusRows
            .GroupBy(s => s.OrderId)
            .ToDictionary(g => g.Key, g => g.OrderByDescending(x => x.ChangedAt).First());

        var lineCounts = await _db.OrderItems.AsNoTracking()
            .Where(i => ids.Contains(i.OrderId))
            .GroupBy(i => i.OrderId)
            .Select(g => new { OrderId = g.Key, Count = g.Count() })
            .ToListAsync(ct);
        var countByOrder = lineCounts.ToDictionary(x => x.OrderId, x => x.Count);

        var fabricLinks = await _db.OrderItems.AsNoTracking()
            .Where(i => ids.Contains(i.OrderId)
                && string.Equals(i.LineSource, "FABRICA")
                && i.FabricaOrderId != null
                && i.ProveedorId != null)
            .Select(i => new FabricLinkRow(i.OrderId, i.ProveedorId!.Value, i.FabricaOrderId!.Value))
            .ToListAsync(ct);

        var (snapCache, provEntities) = await BuildFabricaSnapshotCacheAsync(fabricLinks, ct);

        var keysByOrder = fabricLinks
            .GroupBy(x => x.OrderId)
            .ToDictionary(g => g.Key, g => g.Select(x => (x.ProveedorId, x.FabricaOrderId)).Distinct().ToList());

        return Ok(list.Select(o =>
        {
            latestByOrder.TryGetValue(o.OrderId, out var st);
            var fabList = BuildFabricaStatusListForOrder(o.OrderId, keysByOrder, snapCache, provEntities);
            return new
            {
                orderId = o.OrderId,
                orderNumber = o.OrderNumber,
                userId = o.UserId,
                orderType = o.OrderType,
                subtotal = o.Subtotal,
                shippingTotal = o.ShippingTotal,
                total = o.Total,
                createdAt = o.CreatedAt,
                lineCount = countByOrder.GetValueOrDefault(o.OrderId, 0),
                status = st?.Status ?? "INITIATED",
                trackingNumber = st?.TrackingNumber,
                etaDays = st?.EtaDays,
                fabricaStatuses = fabList
            };
        }));
    }

    /// <summary>Recibo PDF del pedido consolidado en la distribuidora.</summary>
    [HttpGet("{orderId:long}/recibo")]
    public async Task<IActionResult> GetReciboPdf(long orderId, CancellationToken ct)
    {
        var pdf = await _reciboPdf.GenerateAsync(orderId, ct);
        if (pdf == null) return NotFound();
        var order = await _orderService.GetByIdAsync(orderId, ct);
        var filename = order == null ? $"recibo-{orderId}.pdf" : $"recibo-{order.OrderNumber}.pdf";
        return File(pdf, "application/pdf", filename);
    }

    /// <summary>Listar todos los pedidos (solo ADMIN).</summary>
    [HttpGet("todos")]
    public async Task<IActionResult> GetAll([FromQuery] long userId, CancellationToken ct)
    {
        if (!await IsAdminOrEmployeeAsync(userId, ct))
            return Forbid();
        var list = await _orderService.GetAllOrdersAsync(ct);
        return Ok(list.Select(o => new
        {
            orderId = o.OrderId,
            orderNumber = o.OrderNumber,
            userId = o.UserId,
            orderType = o.OrderType,
            subtotal = o.Subtotal,
            shippingTotal = o.ShippingTotal,
            total = o.Total,
            createdAt = o.CreatedAt
        }));
    }

    /// <summary>Actualizar estado del pedido (solo ADMIN/EMPLOYEE). Solo se puede avanzar, no retroceder. Envía correo al cliente.</summary>
    [HttpPatch("{orderId:long}/estado")]
    public async Task<IActionResult> UpdateEstado(long orderId, [FromBody] UpdateEstadoRequest body, CancellationToken ct)
    {
        if (body?.UserId == null || !await IsAdminOrEmployeeAsync(body.UserId.Value, ct))
            return Forbid();
        try
        {
            var newStatus = (body.Status ?? "INITIATED").Trim().ToUpperInvariant();
            var current = await _orderService.GetLatestStatusAsync(orderId, ct);
            var currentStatus = current?.Status?.Trim().ToUpperInvariant() ?? "INITIATED";
            if (!CanAdvanceTo(currentStatus, newStatus))
                return BadRequest(new { message = "No se puede retroceder el estado. Estado actual: " + (current?.Status ?? "Iniciado") + ". Solo se puede avanzar (p. ej. Iniciado → Confirmado → En preparación → Enviado → Entregado)." });

            await _orderService.AddOrderStatusAsync(
                orderId, newStatus, body.Comment, body.TrackingNumber, body.EtaDays, body.UserId.Value, ct);
            var latest = await _orderService.GetLatestStatusAsync(orderId, ct);
            var order = await _orderService.GetByIdAsync(orderId, ct);
            if (order != null)
            {
                var customer = await _db.AppUsers.FindAsync(new object[] { order.UserId }, ct);
                if (customer != null && !string.IsNullOrWhiteSpace(customer.Email))
                {
                    try
                    {
                        _mailService.SendOrderStatusUpdate(
                            customer.Email,
                            customer.FullName,
                            order.OrderNumber,
                            latest?.Status ?? newStatus,
                            body.Comment,
                            body.TrackingNumber,
                            body.EtaDays);
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine("Error enviando correo de actualización de pedido: " + ex.Message);
                    }
                }
            }
            return Ok(new
            {
                status = latest?.Status,
                trackingNumber = latest?.TrackingNumber,
                etaDays = latest?.EtaDays
            });
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
    }

    /// <summary>Orden de estados: solo se permite avanzar (no retroceder). CANCELLED y DELIVERED son finales.</summary>
    private static bool CanAdvanceTo(string currentStatus, string newStatus)
    {
        var order = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase)
        {
            ["INITIATED"] = 0,
            ["CONFIRMED"] = 1,
            ["PREPARING"] = 2,
            ["IN_PREPARATION"] = 2,
            ["SHIPPED"] = 3,
            ["DELIVERED"] = 4,
            ["CANCELLED"] = 99
        };
        if (!order.TryGetValue(newStatus, out var newOrder)) return true;
        if (!order.TryGetValue(currentStatus, out var currentOrder)) return true;
        if (currentOrder == 99) return false;
        if (newStatus == "CANCELLED") return true;
        return newOrder >= currentOrder;
    }

    private async Task<bool> IsAdminAsync(long userId, CancellationToken ct)
    {
        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);
        return user?.UserRoles?.Any(ur => ur.Role?.Name == "ADMIN") ?? false;
    }

    private async Task<bool> IsAdminOrEmployeeAsync(long userId, CancellationToken ct)
    {
        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);
        var names = user?.UserRoles?.Select(ur => ur.Role?.Name).Where(n => n != null).ToHashSet() ?? new HashSet<string?>();
        return names.Contains("ADMIN") || names.Contains("EMPLOYEE");
    }

    /// <summary>Detalle completo del pedido: cabecera, ítems enriquecidos, estado y snapshots remotos de fábrica.</summary>
    [HttpGet("{orderId:long}")]
    public async Task<IActionResult> GetById(long orderId, CancellationToken ct)
    {
        var order = await _orderService.GetByIdAsync(orderId, ct);
        if (order == null) return NotFound();

        var items = await _orderService.GetItemsAsync(orderId, ct);
        var status = await _orderService.GetLatestStatusAsync(orderId, ct);

        var proveedorIds = items.Where(i => i.ProveedorId != null).Select(i => i.ProveedorId!.Value).Distinct().ToList();
        var proveedorUrls = proveedorIds.Count == 0
            ? new Dictionary<long, string?>()
            : await _db.Proveedores.AsNoTracking()
                .Where(p => proveedorIds.Contains(p.ProveedorId))
                .ToDictionaryAsync(p => p.ProveedorId, p => p.ApiBaseUrl, ct);

        var fabricDetailLinks = items
            .Where(i => string.Equals(i.LineSource, "FABRICA", StringComparison.OrdinalIgnoreCase)
                && i.FabricaOrderId != null
                && i.ProveedorId != null)
            .Select(i => new FabricLinkRow(orderId, i.ProveedorId!.Value, i.FabricaOrderId!.Value))
            .ToList();
        var (snapCacheDetail, provEntitiesDetail) = await BuildFabricaSnapshotCacheAsync(fabricDetailLinks, ct);
        var keysSingleOrder = new Dictionary<long, List<(long ProveedorId, long FabricaOrderId)>>
        {
            [orderId] = fabricDetailLinks.Select(x => (x.ProveedorId, x.FabricaOrderId)).Distinct().ToList()
        };
        var fabricaStatusesDetail = BuildFabricaStatusListForOrder(orderId, keysSingleOrder, snapCacheDetail, provEntitiesDetail);

        var itemsWithTitle = new List<object>();
        foreach (var i in items)
        {
            string partTitle;
            if (string.Equals(i.LineSource, "FABRICA", StringComparison.OrdinalIgnoreCase) || i.PartId == null)
                partTitle = i.TitleSnapshot ?? $"Repuesto fábrica #{i.FabricaPartId}";
            else
            {
                var part = await _partService.GetByIdAsync(i.PartId.Value, ct);
                partTitle = part?.Title ?? $"Repuesto #{i.PartId}";
            }

            string? fabricaBase = null;
            if (i.ProveedorId != null && proveedorUrls.TryGetValue(i.ProveedorId.Value, out var u))
                fabricaBase = string.IsNullOrWhiteSpace(u) ? null : u.Trim().TrimEnd('/');

            FabricaPedidoRemoteSnapshot? lineSnap = null;
            if (i.ProveedorId != null && i.FabricaOrderId != null
                && snapCacheDetail.TryGetValue((i.ProveedorId.Value, i.FabricaOrderId.Value), out var s))
                lineSnap = s;

            itemsWithTitle.Add(new
            {
                lineSource = i.LineSource,
                partId = i.PartId,
                fabricaPartId = i.FabricaPartId,
                proveedorId = i.ProveedorId,
                fabricaOrderId = i.FabricaOrderId,
                fabricaBaseUrl = fabricaBase,
                partNumber = i.PartNumberSnapshot,
                partTitle,
                qty = i.Qty,
                unitPrice = i.UnitPrice,
                lineTotal = i.LineTotal,
                fabricaRemoteStatus = lineSnap == null
                    ? null
                    : new { status = lineSnap.Status, trackingNumber = lineSnap.TrackingNumber, etaDays = lineSnap.EtaDays }
            });
        }

        return Ok(new
        {
            order = new
            {
                orderId = order.OrderId,
                orderNumber = order.OrderNumber,
                userId = order.UserId,
                orderType = order.OrderType,
                subtotal = order.Subtotal,
                shippingTotal = order.ShippingTotal,
                total = order.Total,
                createdAt = order.CreatedAt
            },
            items = itemsWithTitle,
            status = status == null ? null : new { status = status.Status, trackingNumber = status.TrackingNumber, etaDays = status.EtaDays },
            fabricaStatuses = fabricaStatusesDetail
        });
    }

    private sealed record FabricLinkRow(long OrderId, long ProveedorId, long FabricaOrderId);

    private async Task<(
        Dictionary<(long ProveedorId, long FabricaOrderId), FabricaPedidoRemoteSnapshot?> SnapCache,
        Dictionary<long, Proveedor> ProvDict)> BuildFabricaSnapshotCacheAsync(
        IReadOnlyList<FabricLinkRow> fabricLinks,
        CancellationToken ct)
    {
        var uniqueFab = fabricLinks.Select(f => (f.ProveedorId, f.FabricaOrderId)).Distinct().ToList();
        var provIdsFab = uniqueFab.Select(x => x.ProveedorId).Distinct().ToList();
        var provEntities = provIdsFab.Count == 0
            ? new Dictionary<long, Proveedor>()
            : await _db.Proveedores.AsNoTracking()
                .Where(p => provIdsFab.Contains(p.ProveedorId))
                .ToDictionaryAsync(p => p.ProveedorId, p => p, ct);

        var snapCache = new Dictionary<(long, long), FabricaPedidoRemoteSnapshot?>();
        foreach (var (provId, foId) in uniqueFab)
        {
            if (snapCache.ContainsKey((provId, foId)))
                continue;
            if (!provEntities.TryGetValue(provId, out var prov) || string.IsNullOrWhiteSpace(prov.ApiBaseUrl))
            {
                snapCache[(provId, foId)] = null;
                continue;
            }

            var snap = await _fabricaIntegration.GetPedidoRemoteStatusAsync(prov.ApiBaseUrl, foId, ct);
            snapCache[(provId, foId)] = snap;
        }

        return (snapCache, provEntities);
    }

    private static List<object> BuildFabricaStatusListForOrder(
        long orderId,
        IReadOnlyDictionary<long, List<(long ProveedorId, long FabricaOrderId)>> keysByOrder,
        IReadOnlyDictionary<(long ProveedorId, long FabricaOrderId), FabricaPedidoRemoteSnapshot?> snapCache,
        IReadOnlyDictionary<long, Proveedor> provEntities)
    {
        var fabList = new List<object>();
        if (!keysByOrder.TryGetValue(orderId, out var keys))
            return fabList;

        var seen = new HashSet<(long, long)>();
        foreach (var (provId, foId) in keys)
        {
            if (!seen.Add((provId, foId)))
                continue;
            if (!snapCache.TryGetValue((provId, foId), out var snap) || snap == null)
                continue;
            if (!provEntities.TryGetValue(provId, out var prov))
                continue;
            fabList.Add(new
            {
                proveedorId = provId,
                proveedorNombre = prov.Nombre,
                fabricaOrderId = foId,
                status = snap.Status,
                trackingNumber = snap.TrackingNumber,
                etaDays = snap.EtaDays
            });
        }

        return fabList;
    }
}
