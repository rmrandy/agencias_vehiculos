using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// API de catálogo de repuestos de la distribuidora: listado, búsqueda, CRUD, imágenes y
/// <see cref="CatalogoUnificado"/> (agregación con proveedores/fábricas activas).
/// </summary>
[ApiController]
[Route("api/repuestos")]
public class RepuestosController : ControllerBase
{
    private readonly PartService _partService;
    private readonly UnifiedCatalogService _unifiedCatalog;

    public RepuestosController(PartService partService, UnifiedCatalogService unifiedCatalog)
    {
        _partService = partService;
        _unifiedCatalog = unifiedCatalog;
    }

    /// <summary>
    /// Catálogo local más catálogos de fábricas activas. Si <paramref name="q"/> está vacío, devuelve todo lo activo agregado.
    /// </summary>
    /// <param name="q">Término de búsqueda opcional.</param>
    /// <param name="ct">Token de cancelación.</param>
    [HttpGet("catalogo/unificado")]
    public async Task<IActionResult> CatalogoUnificado([FromQuery] string? q, CancellationToken ct)
    {
        var term = string.IsNullOrWhiteSpace(q) ? null : q.Trim();
        var list = await _unifiedCatalog.SearchAsync(term, ct);
        return Ok(list);
    }

    /// <summary>Listado paginable lógico de repuestos locales con filtros opcionales.</summary>
    /// <param name="categoryId">Filtrar por categoría.</param>
    /// <param name="brandId">Filtrar por marca.</param>
    /// <param name="includeInactive">Incluir repuestos inactivos.</param>
    [HttpGet]
    public async Task<IActionResult> List([FromQuery] long? categoryId, [FromQuery] long? brandId, [FromQuery] bool includeInactive = false, CancellationToken ct = default)
    {
        var list = await _partService.ListAsync(categoryId, brandId, includeInactive, ct);
        return Ok(list.Select(p => ToDto(p)));
    }

    /// <summary>Búsqueda de texto en nombre, descripción o especificaciones (primer parámetro no nulo entre <paramref name="q"/>, <paramref name="nombre"/>, etc.).</summary>
    [HttpGet("busqueda")]
    public async Task<IActionResult> Busqueda(
        [FromQuery] string? q,
        [FromQuery] string? nombre,
        [FromQuery] string? descripcion,
        [FromQuery] string? especificaciones,
        CancellationToken ct)
    {
        var term = q ?? nombre ?? descripcion ?? especificaciones;
        var list = await _partService.SearchAsync(term, ct);
        return Ok(list.Select(p => ToDto(p)));
    }

    /// <summary>Obtiene un repuesto por su número de parte (<c>partNumber</c>).</summary>
    [HttpGet("numero/{partNumber}")]
    public async Task<IActionResult> GetByPartNumber(string partNumber, CancellationToken ct)
    {
        var part = await _partService.GetByPartNumberAsync(partNumber, ct);
        if (part == null) return NotFound();
        return Ok(ToDto(part));
    }

    /// <summary>Obtiene un repuesto por identificador numérico local.</summary>
    [HttpGet("{id:long}")]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var part = await _partService.GetByIdAsync(id, ct);
        if (part == null) return NotFound();
        return Ok(ToDto(part));
    }

    /// <summary>Galería: cantidad de imágenes (principal + adicionales) para construir URLs /api/images/part/{id}/imagen/0, 1, ...</summary>
    [HttpGet("{id:long}/galeria")]
    public async Task<IActionResult> GetGaleria(long id, CancellationToken ct)
    {
        try
        {
            var count = await _partService.GetGalleryCountAsync(id, ct);
            return Ok(new { count });
        }
        catch (Exception ex)
        {
            Console.WriteLine("[Repuestos] GetGaleria {0}: {1}", id, ex.Message);
            return Ok(new { count = 0 });
        }
    }

    /// <summary>Agregar imagen a la galería del producto (base64).</summary>
    [HttpPost("{id:long}/imagenes")]
    public async Task<IActionResult> AddImagen(long id, [FromBody] AddImagenRequest body, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(body?.ImageData))
            return BadRequest(new { message = "imageData es obligatorio" });
        try
        {
            var (imageData, imageType) = PartService.DecodeImageBase64(body.ImageData, body.ImageType);
            if (imageData.Length == 0) return BadRequest(new { message = "Imagen inválida" });
            await _partService.AddPartImageAsync(id, imageData, imageType, ct);
            var count = await _partService.GetGalleryCountAsync(id, ct);
            return Ok(new { count });
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
        catch (Exception ex)
        {
            Console.WriteLine("[Repuestos] AddImagen error: " + ex.Message);
            return BadRequest(new { message = "No se pudo guardar la imagen. Compruebe el formato (base64) y que la base de datos tenga la tabla PART_IMAGE." });
        }
    }

    /// <summary>Crear repuesto (misma lógica que fábrica). Opcional: imageData (base64), imageType.</summary>
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateRepuestoRequest body, CancellationToken ct)
    {
        if (body == null || string.IsNullOrWhiteSpace(body.PartNumber) || string.IsNullOrWhiteSpace(body.Title))
            return BadRequest(new { message = "partNumber y title son obligatorios" });
        try
        {
            var part = await _partService.CreateAsync(
                body.CategoryId ?? 0,
                body.BrandId ?? 0,
                body.PartNumber,
                body.Title,
                body.Description,
                body.CompatibilityTags,
                body.WeightLb,
                body.Price ?? 0,
                body.StockQuantity ?? 0,
                body.LowStockThreshold ?? 5,
                ct);

            if (!string.IsNullOrWhiteSpace(body.ImageData))
            {
                var (imageData, imageType) = PartService.DecodeImageBase64(body.ImageData, body.ImageType);
                if (imageData.Length > 0)
                    part = await _partService.UpdateImageAsync(part.PartId, imageData, imageType, ct);
            }

            return StatusCode(201, ToDto(part));
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
    }

    /// <summary>Actualizar repuesto (misma lógica que fábrica). Opcional: imageData (base64), imageType.</summary>
    [HttpPut("{id:long}")]
    public async Task<IActionResult> Update(long id, [FromBody] UpdateRepuestoRequest body, CancellationToken ct)
    {
        if (body == null) return BadRequest();
        try
        {
            var part = await _partService.UpdateAsync(id, body.CategoryId, body.BrandId, body.Title, body.Description,
                body.CompatibilityTags,
                body.WeightLb, body.Price, body.Active, ct);

            if (body.StockQuantity.HasValue || body.LowStockThreshold.HasValue)
                part = await _partService.UpdateInventoryAsync(id, body.StockQuantity, body.LowStockThreshold, ct);

            if (!string.IsNullOrWhiteSpace(body.ImageData))
            {
                var (imageData, imageType) = PartService.DecodeImageBase64(body.ImageData, body.ImageType);
                if (imageData.Length > 0)
                    part = await _partService.UpdateImageAsync(id, imageData, imageType, ct);
            }

            return Ok(ToDto(part));
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
    }

    /// <summary>Actualiza stock y umbral de alerta del repuesto.</summary>
    [HttpPut("{id:long}/inventario")]
    public async Task<IActionResult> UpdateInventory(long id, [FromBody] UpdateInventarioRequest body, CancellationToken ct)
    {
        try
        {
            var part = await _partService.UpdateInventoryAsync(id, body.StockQuantity, body.LowStockThreshold, ct);
            return Ok(ToDto(part));
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
    }

    /// <summary>Elimina (o desactiva según implementación del servicio) el repuesto indicado.</summary>
    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken ct)
    {
        try
        {
            await _partService.DeleteAsync(id, ct);
            return NoContent();
        }
        catch { return NotFound(); }
    }

    private static object ToDto(Models.Part p)
    {
        return new
        {
            partId = p.PartId,
            categoryId = p.CategoryId,
            brandId = p.BrandId,
            partNumber = p.PartNumber,
            title = p.Title,
            description = p.Description,
            compatibilityTags = p.CompatibilityTags,
            weightLb = p.WeightLb,
            price = p.Price,
            active = p.Active,
            createdAt = p.CreatedAt,
            hasImage = p.HasImage,
            inStock = p.InStock,
            lowStock = p.LowStock,
            stockQuantity = p.StockQuantity,
            availableQuantity = p.AvailableQuantity
        };
    }
}

/// <summary>Cuerpo JSON para crear repuesto (campos alineados con la fábrica).</summary>
public class CreateRepuestoRequest
{
    public long? CategoryId { get; set; }
    public long? BrandId { get; set; }
    public string PartNumber { get; set; } = "";
    public string Title { get; set; } = "";
    public string? Description { get; set; }
    public string? CompatibilityTags { get; set; }
    public decimal? WeightLb { get; set; }
    public decimal? Price { get; set; }
    public int? StockQuantity { get; set; }
    public int? LowStockThreshold { get; set; }
    public string? ImageData { get; set; }
    public string? ImageType { get; set; }
}

/// <summary>Cuerpo JSON para actualizar repuesto; todos los campos son opcionales salvo uso en lógica de negocio.</summary>
public class UpdateRepuestoRequest
{
    public long? CategoryId { get; set; }
    public long? BrandId { get; set; }
    public string? Title { get; set; }
    public string? Description { get; set; }
    public string? CompatibilityTags { get; set; }
    public decimal? WeightLb { get; set; }
    public decimal? Price { get; set; }
    public int? Active { get; set; }
    public int? StockQuantity { get; set; }
    public int? LowStockThreshold { get; set; }
    public string? ImageData { get; set; }
    public string? ImageType { get; set; }
}

/// <summary>Actualización parcial de inventario.</summary>
public class UpdateInventarioRequest
{
    public int? StockQuantity { get; set; }
    public int? LowStockThreshold { get; set; }
}

/// <summary>Imagen en base64 y tipo MIME opcional para la galería.</summary>
public class AddImagenRequest
{
    public string? ImageData { get; set; }
    public string? ImageType { get; set; }
}
