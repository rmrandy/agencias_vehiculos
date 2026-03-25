using System.Net.Http.Json;
using System.Text.Json;

namespace BackendDistribuidores.Services;

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
            throw new InvalidOperationException($"Búsqueda en fábrica falló ({(int)resp.StatusCode}): {json}");
        return json;
    }

    /// <summary>POST /api/pedidos — devuelve orderId de la fábrica.</summary>
    public async Task<long> CreatePedidoAsync(
        string apiBaseUrl,
        long fabricaUserId,
        IReadOnlyList<(long PartId, int Qty)> items,
        CancellationToken ct)
    {
        if (items.Count == 0)
            throw new ArgumentException("La fábrica requiere al menos un ítem");

        var client = _httpFactory.CreateClient("FabricaIntegration");
        var url = $"{NormalizeBase(apiBaseUrl)}/api/pedidos";
        var body = new
        {
            userId = fabricaUserId,
            items = items.Select(i => new { partId = i.PartId, qty = i.Qty }).ToList()
        };
        using var req = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(body, options: JsonOpts)
        };
        AddApiKey(req);
        using var resp = await client.SendAsync(req, ct);
        var json = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
            throw new InvalidOperationException($"Pedido en fábrica falló ({(int)resp.StatusCode}): {json}");

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        if (!root.TryGetProperty("orderId", out var oid))
            throw new InvalidOperationException("La fábrica no devolvió orderId");
        return oid.GetInt64();
    }
}
