using System.Text.Json;
using BackendDistribuidores.Data;
using BackendDistribuidores.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

/// <summary>Proxy de detalle y comentarios hacia la API de un proveedor (fábrica) por proveedorId.</summary>
[ApiController]
[Route("api/repuestos/fabrica")]
public class RepuestosFabricaController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly FabricaIntegrationService _fabrica;

    public RepuestosFabricaController(AppDbContext db, FabricaIntegrationService fabrica)
    {
        _db = db;
        _fabrica = fabrica;
    }

    [HttpGet("{proveedorId:long}/{partId:long}")]
    public async Task<IActionResult> GetDetalle(long proveedorId, long partId, CancellationToken ct)
    {
        var prov = await ResolveProveedorAsync(proveedorId, ct);
        if (prov == null)
            return NotFound(new { message = "Proveedor no encontrado o inactivo" });

        try
        {
            var json = await _fabrica.GetRepuestoJsonAsync(prov.ApiBaseUrl!, partId, ct);
            var node = System.Text.Json.Nodes.JsonNode.Parse(json);
            if (node is not System.Text.Json.Nodes.JsonObject obj)
                return StatusCode(502, new { message = "Respuesta inválida de la fábrica" });
            var baseUrl = prov.ApiBaseUrl!.Trim().TrimEnd('/');
            obj["source"] = "fabrica";
            obj["proveedorId"] = proveedorId;
            obj["proveedorNombre"] = prov.Nombre;
            obj["fabricaBaseUrl"] = baseUrl;
            return Content(obj.ToJsonString(), "application/json");
        }
        catch (InvalidOperationException ex)
        {
            if (ex.Message.Contains("(404)", StringComparison.Ordinal))
                return NotFound(new { message = "Repuesto no encontrado en la fábrica" });
            return StatusCode(502, new { message = ex.Message });
        }
    }

    [HttpGet("{proveedorId:long}/{partId:long}/comentarios")]
    public async Task<IActionResult> GetComentarios(long proveedorId, long partId, CancellationToken ct)
    {
        var prov = await ResolveProveedorAsync(proveedorId, ct);
        if (prov == null)
            return NotFound(new { message = "Proveedor no encontrado o inactivo" });

        try
        {
            var json = await _fabrica.GetComentariosRepuestoJsonAsync(prov.ApiBaseUrl!, partId, ct);
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            if (!root.TryGetProperty("comentarios", out var arr) || arr.ValueKind != JsonValueKind.Array)
                return Ok(Array.Empty<ComentarioDto>());
            return Ok(MapComentarioRoots(arr));
        }
        catch (InvalidOperationException ex)
        {
            if (ex.Message.Contains("(404)", StringComparison.Ordinal))
                return NotFound(new { message = "Repuesto no encontrado en la fábrica" });
            return StatusCode(502, new { message = ex.Message });
        }
    }

    [HttpPost("{proveedorId:long}/{partId:long}/comentarios")]
    public async Task<IActionResult> CreateComentario(
        long proveedorId,
        long partId,
        [FromBody] CreateComentarioFabricaRequest? body,
        CancellationToken ct)
    {
        if (body == null || string.IsNullOrWhiteSpace(body.Body))
            return BadRequest(new { message = "El comentario no puede estar vacío" });
        if (string.IsNullOrWhiteSpace(body.UserEmail))
            return BadRequest(new { message = "userEmail es obligatorio" });

        var prov = await ResolveProveedorAsync(proveedorId, ct);
        if (prov == null)
            return NotFound(new { message = "Proveedor no encontrado o inactivo" });

        int? rating = body.ParentId == null ? body.Rating : null;
        object payload = new
        {
            userEmail = body.UserEmail.Trim(),
            userFullName = string.IsNullOrWhiteSpace(body.UserFullName) ? null : body.UserFullName.Trim(),
            parentId = body.ParentId,
            rating,
            body = body.Body.Trim()
        };

        try
        {
            var json = await _fabrica.PostComentarioRepuestoJsonAsync(prov.ApiBaseUrl!, partId, payload, ct);
            using var doc = JsonDocument.Parse(json);
            var dto = MapCreatedReview(doc.RootElement, body);
            return StatusCode(201, dto);
        }
        catch (InvalidOperationException ex)
        {
            return StatusCode(502, new { message = ex.Message });
        }
    }

    private async Task<Models.Proveedor?> ResolveProveedorAsync(long proveedorId, CancellationToken ct) =>
        await _db.Proveedores.AsNoTracking()
            .FirstOrDefaultAsync(
                p => p.ProveedorId == proveedorId && p.Activo && p.ApiBaseUrl != null && p.ApiBaseUrl != "",
                ct);

    private static List<ComentarioDto> MapComentarioRoots(JsonElement arr)
    {
        var list = new List<ComentarioDto>();
        foreach (var el in arr.EnumerateArray())
            list.Add(MapComentarioNode(el));
        return list;
    }

    private static ComentarioDto MapComentarioNode(JsonElement el)
    {
        var dto = new ComentarioDto
        {
            reviewId = ReadInt64(el, "reviewId"),
            partId = ReadInt64(el, "partId"),
            userId = ReadInt64(el, "userId"),
            fullName = TryString(el, "fullName") ?? TryString(el, "userDisplayName"),
            userEmail = TryString(el, "userEmail"),
            parentId = TryLongNullable(el, "parentId"),
            rating = TryIntNullable(el, "rating"),
            body = TryString(el, "body") ?? "",
            createdAt = ParseDateTime(el, "createdAt"),
            children = new List<ComentarioDto>()
        };
        if (el.TryGetProperty("children", out var ch) && ch.ValueKind == JsonValueKind.Array)
        {
            foreach (var c in ch.EnumerateArray())
                dto.children.Add(MapComentarioNode(c));
        }

        return dto;
    }

    private static ComentarioDto MapCreatedReview(JsonElement el, CreateComentarioFabricaRequest req)
    {
        return new ComentarioDto
        {
            reviewId = ReadInt64(el, "reviewId"),
            partId = ReadInt64(el, "partId"),
            userId = ReadInt64(el, "userId"),
            userEmail = req.UserEmail!.Trim(),
            fullName = string.IsNullOrWhiteSpace(req.UserFullName) ? null : req.UserFullName.Trim(),
            parentId = TryLongNullable(el, "parentId"),
            rating = TryIntNullable(el, "rating"),
            body = TryString(el, "body") ?? "",
            createdAt = ParseDateTime(el, "createdAt"),
            children = new List<ComentarioDto>()
        };
    }

    private static long ReadInt64(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind != JsonValueKind.Number)
            return 0;
        return p.GetInt64();
    }

    private static string? TryString(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind != JsonValueKind.String)
            return null;
        return p.GetString();
    }

    private static long? TryLongNullable(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind == JsonValueKind.Null)
            return null;
        if (p.ValueKind == JsonValueKind.Number)
            return p.GetInt64();
        return null;
    }

    private static int? TryIntNullable(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p) || p.ValueKind == JsonValueKind.Null)
            return null;
        if (p.ValueKind == JsonValueKind.Number)
            return p.GetInt32();
        return null;
    }

    private static DateTime? ParseDateTime(JsonElement el, string name)
    {
        if (!el.TryGetProperty(name, out var p))
            return null;
        if (p.ValueKind == JsonValueKind.String && DateTime.TryParse(p.GetString(), out var dt))
            return dt;
        if (p.ValueKind == JsonValueKind.Number)
        {
            try
            {
                var n = p.GetInt64();
                if (n > 1_000_000_000_000L)
                    return DateTimeOffset.FromUnixTimeMilliseconds(n).UtcDateTime;
                return DateTimeOffset.FromUnixTimeSeconds(n).UtcDateTime;
            }
            catch
            {
                return null;
            }
        }

        return null;
    }
}

/// <summary>Crear comentario en fábrica (el backend reenvía userEmail con la API key configurada).</summary>
public class CreateComentarioFabricaRequest
{
    public string? UserEmail { get; set; }
    public string? UserFullName { get; set; }
    public long? ParentId { get; set; }
    public int? Rating { get; set; }
    public string? Body { get; set; }
}
