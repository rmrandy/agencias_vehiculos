using Microsoft.AspNetCore.Mvc;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// Comprobación de vida del servicio (<c>GET /api/health</c>).
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class HealthController : ControllerBase
{
    /// <summary>Indica que el proceso responde correctamente.</summary>
    /// <returns>200 con <c>{"status":"ok"}</c>.</returns>
    [HttpGet]
    public IActionResult Get()
    {
        return Ok(new { status = "ok" });
    }
}
