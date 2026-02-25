using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/fabrica/auth")]
public class FabricaAuthController : ControllerBase
{
    private readonly FabricaProxyService _fabrica;

    public FabricaAuthController(FabricaProxyService fabrica)
    {
        _fabrica = fabrica;
    }

    /// <summary>Login contra la fábrica. Valida usuario empresarial (email + password).</summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] FabricaLoginRequest request, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Password))
            return BadRequest(new { message = "Email y contraseña son obligatorios" });

        var res = await _fabrica.PostAsync("/api/auth/login", new { email = request.Email, password = request.Password }, ct);
        var content = await res.Content.ReadAsStringAsync(ct);
        if (!res.IsSuccessStatusCode)
        {
            return StatusCode((int)res.StatusCode, content);
        }
        return Content(content, "application/json");
    }
}

public class FabricaLoginRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
}
