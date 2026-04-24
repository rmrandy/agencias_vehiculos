using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Services;

/// <summary>Último estado del pedido en la fábrica (GET /api/pedidos/{id}).</summary>
public sealed record FabricaPedidoRemoteSnapshot(string? Status, string? TrackingNumber, int? EtaDays);

/// <summary>Entrada del historial de estados en fábrica (GET /api/pedidos/{id}/historial).</summary>
public sealed record FabricaHistorialEntry(
    string? Status,
    string? CommentText,
    string? TrackingNumber,
    int? EtaDays,
    DateTimeOffset? ChangedAt);

/// <summary>HTTP hacia APIs de fábrica (cada proveedor con su propia base URL).</summary>
public sealed class FabricaIntegrationService
{
    private readonly IHttpClientFactory _httpFactory;
    private readonly IConfiguration _config;

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNameCaseInsensitive = true,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
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

    /// <summary>GET /api/repuestos/{partId} — detalle JSON (sin binarios de imagen en el modelo Java).</summary>
    public async Task<string> GetRepuestoJsonAsync(string apiBaseUrl, long partId, CancellationToken ct)
    {
        var client = _httpFactory.CreateClient("FabricaIntegration");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/repuestos/{partId}";
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        AddApiKey(req);
        using var resp = await client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
            throw new InvalidOperationException(
                $"Repuesto en fábrica falló ({(int)resp.StatusCode}) URL={url}: {json}");
        return json;
    }

    /// <summary>GET /api/repuestos/{partId}/comentarios — cuerpo JSON (promedio + comentarios).</summary>
    public async Task<string> GetComentariosRepuestoJsonAsync(string apiBaseUrl, long partId, CancellationToken ct)
    {
        var client = _httpFactory.CreateClient("FabricaIntegration");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/repuestos/{partId}/comentarios";
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        AddApiKey(req);
        using var resp = await client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
            throw new InvalidOperationException(
                $"Comentarios en fábrica fallaron ({(int)resp.StatusCode}) URL={url}: {json}");
        return json;
    }

    /// <summary>POST /api/repuestos/{partId}/comentarios — crea comentario (userEmail vía API key en fábrica).</summary>
    public async Task<string> PostComentarioRepuestoJsonAsync(
        string apiBaseUrl,
        long partId,
        object body,
        long? distributorUserId,
        CancellationToken ct)
    {
        var client = _httpFactory.CreateClient("FabricaIntegration");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/repuestos/{partId}/comentarios";
        using var req = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(body, options: JsonOpts)
        };
        AddApiKey(req);
        if (distributorUserId.HasValue && distributorUserId.Value > 0)
            req.Headers.TryAddWithoutValidation("X-Distributor-User-Id", distributorUserId.Value.ToString());
        using var resp = await client.SendAsync(req, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
        {
            var detail = TryExtractMessage(json) ?? json;
            throw new InvalidOperationException($"Comentario en fábrica falló ({(int)resp.StatusCode}): {detail}");
        }

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

    /// <summary>GET /api/pedidos/{fabricaOrderId}/historial — mensajes y estados registrados en la fábrica.</summary>
    public async Task<IReadOnlyList<FabricaHistorialEntry>> GetPedidoHistorialRemoteAsync(
        string apiBaseUrl,
        long fabricaOrderId,
        CancellationToken ct)
    {
        try
        {
            var client = _httpFactory.CreateClient("FabricaIntegration");
            var url = $"{NormalizeBase(apiBaseUrl)}/api/pedidos/{fabricaOrderId}/historial";
            using var req = new HttpRequestMessage(HttpMethod.Get, url);
            AddApiKey(req);
            using var resp = await client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
            var json = await resp.Content.ReadAsStringAsync(ct);
            if (!resp.IsSuccessStatusCode)
                return Array.Empty<FabricaHistorialEntry>();
            using var doc = JsonDocument.Parse(json);
            if (doc.RootElement.ValueKind != JsonValueKind.Array)
                return Array.Empty<FabricaHistorialEntry>();

            var raw = new List<FabricaHistorialEntry>();
            foreach (var el in doc.RootElement.EnumerateArray())
                raw.Add(ParseHistorialElement(el));

            return raw
                .OrderBy(e => e.ChangedAt ?? DateTimeOffset.MinValue)
                .ThenBy(e => e.Status)
                .ToList();
        }
        catch (Exception ex)
        {
            Console.WriteLine("[FabricaIntegration] GetPedidoHistorial: " + ex.Message);
            return Array.Empty<FabricaHistorialEntry>();
        }
    }

    private static FabricaHistorialEntry ParseHistorialElement(JsonElement el)
    {
        string? status = TryGetString(el, "status");
        string? comment = TryGetString(el, "commentText") ?? TryGetString(el, "comment_text");
        string? tracking = TryGetString(el, "trackingNumber") ?? TryGetString(el, "tracking_number");
        int? eta = null;
        if (el.TryGetProperty("etaDays", out var et) && et.ValueKind == JsonValueKind.Number)
            eta = et.GetInt32();
        else if (el.TryGetProperty("eta_days", out var et2) && et2.ValueKind == JsonValueKind.Number)
            eta = et2.GetInt32();
        var changed = TryParseHistorialDate(el, "changedAt") ?? TryParseHistorialDate(el, "changed_at");
        return new FabricaHistorialEntry(status, comment, tracking, eta, changed);
    }

    private static string? TryGetString(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p))
            return null;
        return p.ValueKind == JsonValueKind.String ? p.GetString() : null;
    }

    private static DateTimeOffset? TryParseHistorialDate(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p))
            return null;
        if (p.ValueKind == JsonValueKind.String && DateTimeOffset.TryParse(p.GetString(), out var dto))
            return dto;
        if (p.ValueKind == JsonValueKind.Number)
        {
            try
            {
                var n = p.GetInt64();
                if (n > 1_000_000_000_000L)
                    return DateTimeOffset.FromUnixTimeMilliseconds(n);
                return DateTimeOffset.FromUnixTimeSeconds(n);
            }
            catch
            {
                /* ignore */
            }
        }

        return null;
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
