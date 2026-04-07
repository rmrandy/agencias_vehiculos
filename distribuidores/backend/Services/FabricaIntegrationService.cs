using System.Net.Http.Json;
using System.Text.Json;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Services;

/// <summary>Último estado del pedido en la fábrica (GET /api/pedidos/{id}).</summary>
public sealed record FabricaPedidoRemoteSnapshot(string? Status, string? TrackingNumber, int? EtaDays);

/// <summary>HTTP hacia APIs de fábrica (cada proveedor con su propia base URL).</summary>
public sealed class FabricaIntegrationService
{
    private readonly IHttpClientFactory _httpFactory;
    private readonly IConfiguration _config;

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public FabricaIntegrationService(IHttpClientFactory httpFactory, IConfiguration config)
    {
        _httpFactory = httpFactory;
        _config = config;
    }

    private string? OutboundApiKey => _config["Integration:DistributorApiKey"];

    private static string NormalizeBase(string url) => url.Trim().TrimEnd('/');

    private void AddApiKey(HttpRequestMessage req)
    {
        var key = OutboundApiKey;
        if (!string.IsNullOrWhiteSpace(key))
            req.Headers.TryAddWithoutValidation("X-Distributor-Api-Key", key.Trim());
    }

    /// <summary>GET /api/repuestos/busqueda?nombre=… — cuerpo JSON (array de repuestos).</summary>
    public async Task<string> GetBusquedaRepuestosJsonAsync(string apiBaseUrl, string nombre, CancellationToken ct)
    {
        var client = _httpFactory.CreateClient("FabricaIntegration");
        var q = Uri.EscapeDataString(nombre ?? "");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/repuestos/busqueda?nombre={q}";
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        AddApiKey(req);
        using var resp = await client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
            throw new InvalidOperationException(
                $"Búsqueda en fábrica falló ({(int)resp.StatusCode}) URL={url}: {json}");
        return json;
    }

    /// <summary>POST /api/pedidos — devuelve orderId de la fábrica. Si hay payment, la fábrica valida tarjeta y puede enviar correo.</summary>
    public async Task<long> CreatePedidoAsync(
        string apiBaseUrl,
        long fabricaUserId,
        IReadOnlyList<(long PartId, int Qty)> items,
        PaymentRequest? payment,
        CancellationToken ct)
    {
        if (items.Count == 0)
            throw new ArgumentException("La fábrica requiere al menos un ítem");

        var client = _httpFactory.CreateClient("FabricaIntegration");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/pedidos";
        object body = payment != null
            ? new
            {
                userId = fabricaUserId,
                items = items.Select(i => new { partId = i.PartId, qty = i.Qty }).ToList(),
                payment = new
                {
                    cardNumber = payment.CardNumber,
                    expiryMonth = payment.ExpiryMonth,
                    expiryYear = payment.ExpiryYear
                }
            }
            : new
            {
                userId = fabricaUserId,
                items = items.Select(i => new { partId = i.PartId, qty = i.Qty }).ToList()
            };
        using var req = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(body, options: JsonOpts)
        };
        AddApiKey(req);
        req.Headers.TryAddWithoutValidation("X-Order-Origin", "distributor");
        using var resp = await client.SendAsync(req, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
        {
            var detail = TryExtractMessage(json) ?? json;
            throw new InvalidOperationException($"Pedido en fábrica falló ({(int)resp.StatusCode}): {detail}");
        }

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        if (!root.TryGetProperty("orderId", out var oid))
            throw new InvalidOperationException("La fábrica no devolvió orderId");
        return oid.GetInt64();
    }

    /// <summary>GET /api/pedidos/{fabricaOrderId} — último estado (para sincronizar la distribuidora).</summary>
    public async Task<FabricaPedidoRemoteSnapshot?> GetPedidoRemoteStatusAsync(
        string apiBaseUrl,
        long fabricaOrderId,
        CancellationToken ct)
    {
        try
        {
            var client = _httpFactory.CreateClient("FabricaIntegration");
            var url = $"{NormalizeBase(apiBaseUrl)}/api/pedidos/{fabricaOrderId}";
            using var req = new HttpRequestMessage(HttpMethod.Get, url);
            AddApiKey(req);
            using var resp = await client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
            var json = await resp.Content.ReadAsStringAsync(ct);
            if (!resp.IsSuccessStatusCode)
                return null;
            using var doc = JsonDocument.Parse(json);
            if (!doc.RootElement.TryGetProperty("status", out var st) || st.ValueKind != JsonValueKind.Object)
                return null;
            string? statusStr = st.TryGetProperty("status", out var sv) && sv.ValueKind == JsonValueKind.String
                ? sv.GetString()
                : null;
            string? tracking = st.TryGetProperty("trackingNumber", out var tr) && tr.ValueKind == JsonValueKind.String
                ? tr.GetString()
                : null;
            int? eta = null;
            if (st.TryGetProperty("etaDays", out var et) && et.ValueKind == JsonValueKind.Number)
                eta = et.GetInt32();
            return new FabricaPedidoRemoteSnapshot(statusStr, tracking, eta);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[FabricaIntegration] GetPedidoRemoteStatus: " + ex.Message);
            return null;
        }
    }

    private static string? TryExtractMessage(string json)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            if (root.TryGetProperty("message", out var m) && m.ValueKind == JsonValueKind.String)
                return m.GetString();
        }
        catch
        {
            /* ignore */
        }
        return null;
    }
}
