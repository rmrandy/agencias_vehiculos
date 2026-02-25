using System.Net.Http.Json;
using System.Text;
using System.Text.Json;

namespace BackendDistribuidores.Services;

/// <summary>
/// Proxy hacia la API de la Fábrica (usuarios empresariales, repuestos, pedidos).
/// </summary>
public class FabricaProxyService
{
    private readonly HttpClient _http;
    private readonly string _baseUrl;

    public FabricaProxyService(HttpClient http, IConfiguration config)
    {
        _http = http;
        _baseUrl = (config["FabricaApiUrl"] ?? "http://localhost:8080").TrimEnd('/');
    }

    public async Task<HttpResponseMessage> PostAsync(string path, object? body, CancellationToken ct = default)
    {
        var req = new HttpRequestMessage(HttpMethod.Post, _baseUrl + path);
        if (body != null)
        {
            req.Content = new StringContent(
                JsonSerializer.Serialize(body),
                Encoding.UTF8,
                "application/json");
        }
        return await _http.SendAsync(req, ct);
    }

    public async Task<HttpResponseMessage> GetAsync(string path, CancellationToken ct = default)
    {
        return await _http.GetAsync(_baseUrl + path, ct);
    }

    public async Task<HttpResponseMessage> PutAsync(string path, object? body, CancellationToken ct = default)
    {
        var req = new HttpRequestMessage(HttpMethod.Put, _baseUrl + path);
        if (body != null)
        {
            req.Content = new StringContent(
                JsonSerializer.Serialize(body),
                Encoding.UTF8,
                "application/json");
        }
        return await _http.SendAsync(req, ct);
    }
}
