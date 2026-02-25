using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/images")]
public class ImagesController : ControllerBase
{
    private readonly PartService _partService;

    public ImagesController(PartService partService)
    {
        _partService = partService;
    }

    [HttpGet("part/{id:long}")]
    public async Task<IActionResult> GetPartImage(long id, CancellationToken ct)
    {
        var result = await _partService.GetPartImageByIndexAsync(id, 0, ct);
        if (result == null) return NotFound();
        var contentType = !string.IsNullOrEmpty(result.Value.type) ? result.Value.type : "image/jpeg";
        return File(result.Value.data, contentType);
    }

    /// <summary>Imagen de la galería por índice (0 = principal, 1+ = adicionales).</summary>
    [HttpGet("part/{partId:long}/imagen/{index:int}")]
    public async Task<IActionResult> GetPartImageByIndex(long partId, int index, CancellationToken ct)
    {
        if (index < 0) return NotFound();
        var result = await _partService.GetPartImageByIndexAsync(partId, index, ct);
        if (result == null) return NotFound();
        var contentType = !string.IsNullOrEmpty(result.Value.type) ? result.Value.type : "image/jpeg";
        return File(result.Value.data, contentType);
    }

    /// <summary>Validar imagen en base64 (misma lógica que fábrica: max 5MB, tipos válidos).</summary>
    [HttpPost("validate")]
    public IActionResult Validate([FromBody] ValidateImageRequest? body)
    {
        if (body?.ImageData == null || string.IsNullOrWhiteSpace(body.ImageData))
            return BadRequest(new { message = "No se proporcionó imagen" });
        try
        {
            var (data, _) = PartService.DecodeImageBase64(body.ImageData, body.ImageType);
            return Ok(new { valid = true, size = data.Length, sizeKB = data.Length / 1024 });
        }
        catch (ArgumentException e)
        {
            return BadRequest(new { message = e.Message });
        }
    }
}

public class ValidateImageRequest
{
    public string? ImageData { get; set; }
    public string? ImageType { get; set; }
}
