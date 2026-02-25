using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AuthService _auth;

    public AuthController(AuthService auth)
    {
        _auth = auth;
    }

    /// <summary>Login contra base de datos local (misma lógica que fábrica: email + password).</summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Password))
            return BadRequest(new { message = "Email y contraseña son obligatorios" });

        var user = await _auth.LoginAsync(request.Email, request.Password, ct);
        if (user == null)
            return Unauthorized(new { message = "Credenciales incorrectas" });

        var roles = user.UserRoles?.Select(ur => ur.Role?.Name).Where(n => n != null).Cast<string>().ToList() ?? new List<string>();
        return Ok(new
        {
            userId = user.UserId,
            email = user.Email,
            fullName = user.FullName,
            phone = user.Phone,
            status = user.Status,
            roles
        });
    }

    /// <summary>Registro de usuario para el portal (opcional).</summary>
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request, CancellationToken ct)
    {
        try
        {
            var user = await _auth.RegisterAsync(
                request.Email ?? "",
                request.Password ?? "",
                request.FullName,
                request.Phone,
                ct);
            return Ok(new { userId = user.UserId, email = user.Email, message = "Registrado correctamente" });
        }
        catch (ArgumentException e) { return BadRequest(new { message = e.Message }); }
        catch (InvalidOperationException e) { return BadRequest(new { message = e.Message }); }
    }
}

public class LoginRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
}

public class RegisterRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
    public string? FullName { get; set; }
    public string? Phone { get; set; }
}
