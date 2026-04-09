using Microsoft.AspNetCore.Mvc;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// Autenticación y registro de usuarios del portal de la distribuidora (credenciales en BD local, hash BCrypt).
/// </summary>
[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AuthService _auth;

    public AuthController(AuthService auth)
    {
        _auth = auth;
    }

    /// <summary>Autentica por email y contraseña contra la base local.</summary>
    /// <param name="request">Credenciales del usuario.</param>
    /// <param name="ct">Token de cancelación.</param>
    /// <returns>200 con datos de usuario y roles; 401 si falla la autenticación.</returns>
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

    /// <summary>Crea una cuenta de usuario en la distribuidora.</summary>
    /// <param name="request">Email, contraseña y datos de perfil opcionales.</param>
    /// <param name="ct">Token de cancelación.</param>
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

/// <summary>DTO de entrada para <c>POST /api/auth/login</c>.</summary>
public class LoginRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
}

/// <summary>DTO de entrada para <c>POST /api/auth/register</c>.</summary>
public class RegisterRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
    public string? FullName { get; set; }
    public string? Phone { get; set; }
}
