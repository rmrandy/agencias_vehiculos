using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/proveedores")]
public class ProveedoresController : ControllerBase
{
    private readonly AppDbContext _db;

    public ProveedoresController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] bool incluirInactivos = true, CancellationToken ct = default)
    {
        var query = _db.Proveedores.AsQueryable();
        if (!incluirInactivos)
            query = query.Where(p => p.Activo);

        var proveedores = await query
            .OrderBy(p => p.Nombre)
            .Select(p => ToDto(p))
            .ToListAsync(ct);

        return Ok(proveedores);
    }

    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var proveedor = await _db.Proveedores.FindAsync(new object[] { id }, ct);
        if (proveedor == null) return NotFound(new { message = "Proveedor no encontrado" });
        return Ok(ToDto(proveedor));
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] SaveProveedorRequest body, CancellationToken ct)
    {
        var error = Validate(body);
        if (error != null) return BadRequest(new { message = error });

        var proveedor = new Proveedor
        {
            Nombre = body.Nombre!.Trim(),
            Contacto = Normalize(body.Contacto),
            Email = Normalize(body.Email),
            Telefono = Normalize(body.Telefono),
            ApiBaseUrl = Normalize(body.ApiBaseUrl),
            FabricaEnterpriseUserId = body.FabricaEnterpriseUserId,
            TipoCambioAQuetzales = body.TipoCambioAQuetzales,
            PorcentajeGanancia = body.PorcentajeGanancia,
            CostoEnvioPorLibra = body.CostoEnvioPorLibra,
            Activo = body.Activo ?? true
        };

        _db.Proveedores.Add(proveedor);
        await _db.SaveChangesAsync(ct);

        return StatusCode(201, ToDto(proveedor));
    }

    [HttpPut("{id:long}")]
    public async Task<IActionResult> Update(long id, [FromBody] SaveProveedorRequest body, CancellationToken ct)
    {
        var proveedor = await _db.Proveedores.FindAsync(new object[] { id }, ct);
        if (proveedor == null) return NotFound(new { message = "Proveedor no encontrado" });

        var error = Validate(body);
        if (error != null) return BadRequest(new { message = error });

        proveedor.Nombre = body.Nombre!.Trim();
        proveedor.Contacto = Normalize(body.Contacto);
        proveedor.Email = Normalize(body.Email);
        proveedor.Telefono = Normalize(body.Telefono);
        proveedor.ApiBaseUrl = Normalize(body.ApiBaseUrl);
        proveedor.FabricaEnterpriseUserId = body.FabricaEnterpriseUserId;
        proveedor.TipoCambioAQuetzales = body.TipoCambioAQuetzales;
        proveedor.PorcentajeGanancia = body.PorcentajeGanancia;
        proveedor.CostoEnvioPorLibra = body.CostoEnvioPorLibra;
        proveedor.Activo = body.Activo ?? proveedor.Activo;

        await _db.SaveChangesAsync(ct);
        return Ok(ToDto(proveedor));
    }

    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken ct)
    {
        var proveedor = await _db.Proveedores.FindAsync(new object[] { id }, ct);
        if (proveedor == null) return NotFound(new { message = "Proveedor no encontrado" });

        _db.Proveedores.Remove(proveedor);
        await _db.SaveChangesAsync(ct);
        return NoContent();
    }

    private static object ToDto(Proveedor p)
    {
        var esInternacional =
            p.TipoCambioAQuetzales.HasValue ||
            p.PorcentajeGanancia.HasValue ||
            p.CostoEnvioPorLibra.HasValue;

        return new
        {
            proveedorId = p.ProveedorId,
            nombre = p.Nombre,
            contacto = p.Contacto,
            email = p.Email,
            telefono = p.Telefono,
            apiBaseUrl = p.ApiBaseUrl,
            fabricaEnterpriseUserId = p.FabricaEnterpriseUserId,
            tipoCambioAQuetzales = p.TipoCambioAQuetzales,
            porcentajeGanancia = p.PorcentajeGanancia,
            costoEnvioPorLibra = p.CostoEnvioPorLibra,
            activo = p.Activo,
            esInternacional
        };
    }

    private static string? Validate(SaveProveedorRequest? body)
    {
        if (body == null) return "Solicitud inválida";
        if (string.IsNullOrWhiteSpace(body.Nombre)) return "El nombre es obligatorio";

        if (body.TipoCambioAQuetzales.HasValue && body.TipoCambioAQuetzales.Value <= 0)
            return "tipoCambioAQuetzales debe ser mayor que 0";
        if (body.PorcentajeGanancia.HasValue && body.PorcentajeGanancia.Value < 0)
            return "porcentajeGanancia no puede ser negativo";
        if (body.CostoEnvioPorLibra.HasValue && body.CostoEnvioPorLibra.Value < 0)
            return "costoEnvioPorLibra no puede ser negativo";

        var anyInternationalField =
            body.TipoCambioAQuetzales.HasValue ||
            body.PorcentajeGanancia.HasValue ||
            body.CostoEnvioPorLibra.HasValue;

        // Si se define como internacional, exigimos tipo de cambio (dato mínimo para DOC2).
        if (anyInternationalField && !body.TipoCambioAQuetzales.HasValue)
            return "Para proveedor internacional debe definir tipoCambioAQuetzales";

        return null;
    }

    private static string? Normalize(string? value)
    {
        return string.IsNullOrWhiteSpace(value) ? null : value.Trim();
    }
}

public class SaveProveedorRequest
{
    public string? Nombre { get; set; }
    public string? Contacto { get; set; }
    public string? Email { get; set; }
    public string? Telefono { get; set; }
    public string? ApiBaseUrl { get; set; }
    public long? FabricaEnterpriseUserId { get; set; }
    public decimal? TipoCambioAQuetzales { get; set; }
    public decimal? PorcentajeGanancia { get; set; }
    public decimal? CostoEnvioPorLibra { get; set; }
    public bool? Activo { get; set; }
}
