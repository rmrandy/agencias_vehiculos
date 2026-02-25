using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/pedidos")]
public class PedidosController : ControllerBase
{
    private static readonly Regex OnlyDigits = new(@"^\d+$");

    private readonly OrderService _orderService;
    private readonly PartService _partService;
    private readonly MailService _mailService;
    private readonly AppDbContext _db;

    public PedidosController(OrderService orderService, PartService partService, MailService mailService, AppDbContext db)
    {
        _orderService = orderService;
        _partService = partService;
        _mailService = mailService;
        _db = db;
    }

    /// <summary>Crear pedido. Opcional: payment (validación tarjeta, misma lógica que fábrica).</summary>
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
            var items = request.Items
                .Select(i => new OrderItemDto { PartId = i.PartId, Qty = i.Qty })
                .ToList();
            var order = await _orderService.CreateOrderAsync(request.UserId.Value, items, ct);

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
                            var part = await _partService.GetByIdAsync(i.PartId, ct);
                            emailItems.Add((part?.Title ?? "Repuesto #" + i.PartId, i.Qty, i.UnitPrice, i.LineTotal));
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

    [HttpGet("usuario/{userId:long}")]
    public async Task<IActionResult> GetByUser(long userId, CancellationToken ct)
    {
        var list = await _orderService.GetByUserIdAsync(userId, ct);
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

    /// <summary>Actualizar estado del pedido (solo ADMIN/EMPLOYEE). Envía correo al cliente con la actualización.</summary>
    [HttpPatch("{orderId:long}/estado")]
    public async Task<IActionResult> UpdateEstado(long orderId, [FromBody] UpdateEstadoRequest body, CancellationToken ct)
    {
        if (body?.UserId == null || !await IsAdminOrEmployeeAsync(body.UserId.Value, ct))
            return Forbid();
        try
        {
            await _orderService.AddOrderStatusAsync(
                orderId, body.Status ?? "INITIATED", body.Comment, body.TrackingNumber, body.EtaDays, body.UserId.Value, ct);
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
                            latest?.Status ?? body.Status ?? "INITIATED",
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

    [HttpGet("{orderId:long}")]
    public async Task<IActionResult> GetById(long orderId, CancellationToken ct)
    {
        var order = await _orderService.GetByIdAsync(orderId, ct);
        if (order == null) return NotFound();

        var items = await _orderService.GetItemsAsync(orderId, ct);
        var status = await _orderService.GetLatestStatusAsync(orderId, ct);

        var itemsWithTitle = new List<object>();
        foreach (var i in items)
        {
            var part = await _partService.GetByIdAsync(i.PartId, ct);
            itemsWithTitle.Add(new
            {
                partId = i.PartId,
                partTitle = part?.Title ?? $"Repuesto #{i.PartId}",
                qty = i.Qty,
                unitPrice = i.UnitPrice,
                lineTotal = i.LineTotal
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
            status = status == null ? null : new { status = status.Status, trackingNumber = status.TrackingNumber, etaDays = status.EtaDays }
        });
    }
}

public class CreatePedidoRequest
{
    public long? UserId { get; set; }
    public List<PedidoItemRequest>? Items { get; set; }
    public PaymentRequest? Payment { get; set; }
}

public class PaymentRequest
{
    public string? CardNumber { get; set; }
    public int? ExpiryMonth { get; set; }
    public int? ExpiryYear { get; set; }
}

public class PedidoItemRequest
{
    public long PartId { get; set; }
    public int Qty { get; set; }
}

public class UpdateEstadoRequest
{
    public long? UserId { get; set; }
    public string? Status { get; set; }
    public string? Comment { get; set; }
    public string? TrackingNumber { get; set; }
    public int? EtaDays { get; set; }
}
