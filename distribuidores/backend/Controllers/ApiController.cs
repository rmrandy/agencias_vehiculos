using Microsoft.AspNetCore.Mvc;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// Raíz del API REST (<c>GET /api</c>): enlaces a health, diagnóstico de BD y recurso legacy de distribuidores.
/// </summary>
[ApiController]
[Route("api")]
public class ApiController : ControllerBase
{
    /// <summary>Devuelve un objeto JSON con rutas útiles del servicio.</summary>
    /// <returns>200 con propiedades <c>api</c>, <c>health</c>, <c>db</c>, <c>distribuidores</c>.</returns>
    [HttpGet]
    public IActionResult Get()
    {
        return Ok(new
        {
            api = "ok",
            health = "/api/health",
            db = "/api/db",
            distribuidores = "/api/distribuidores"
        });
    }
}
