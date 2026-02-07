using Microsoft.AspNetCore.Mvc;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api")]
public class ApiController : ControllerBase
{
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
