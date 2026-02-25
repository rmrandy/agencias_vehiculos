using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/usuarios")]
public class UsuariosController : ControllerBase
{
    private readonly AppDbContext _db;

    public UsuariosController(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Listar todos los usuarios (solo ADMIN).</summary>
    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] long userId, CancellationToken ct)
    {
        if (!await IsAdminAsync(userId, ct))
            return StatusCode(403, new { message = "Requiere rol ADMIN" });
        var users = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .OrderBy(u => u.Email)
            .ToListAsync(ct);
        return Ok(users.Select(u => new
        {
            userId = u.UserId,
            email = u.Email,
            fullName = u.FullName,
            phone = u.Phone,
            status = u.Status,
            createdAt = u.CreatedAt,
            roles = u.UserRoles?.Select(ur => ur.Role?.Name).Where(n => n != null).Cast<string>().ToList() ?? new List<string>()
        }));
    }

    /// <summary>Actualizar estado o roles de un usuario (solo ADMIN).</summary>
    [HttpPatch("{id:long}")]
    public async Task<IActionResult> Update(long id, [FromBody] UpdateUsuarioRequest body, CancellationToken ct)
    {
        if (body?.AdminUserId == null || !await IsAdminAsync(body.AdminUserId.Value, ct))
            return StatusCode(403, new { message = "Requiere rol ADMIN" });
        var user = await _db.AppUsers.FindAsync(new object[] { id }, ct);
        if (user == null) return NotFound();
        if (body.Status != null) user.Status = body.Status;
        if (body.RoleNames != null)
        {
            var roles = await _db.Roles.ToListAsync(ct);
            var toAssign = body.RoleNames.Where(n => !string.IsNullOrWhiteSpace(n)).Select(n => n!.Trim()).Distinct().ToList();
            var current = await _db.UserRoles.Where(ur => ur.UserId == id).ToListAsync(ct);
            _db.UserRoles.RemoveRange(current);
            foreach (var name in toAssign)
            {
                var role = roles.FirstOrDefault(r => r.Name == name);
                if (role != null)
                    _db.UserRoles.Add(new UserRole { UserId = id, RoleId = role.RoleId });
            }
        }
        await _db.SaveChangesAsync(ct);
        return Ok(new { userId = user.UserId, status = user.Status });
    }

    private async Task<bool> IsAdminAsync(long userId, CancellationToken ct)
    {
        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);
        return user?.UserRoles?.Any(ur => ur.Role?.Name == "ADMIN") ?? false;
    }
}

public class UpdateUsuarioRequest
{
    public long? AdminUserId { get; set; }
    public string? Status { get; set; }
    public List<string>? RoleNames { get; set; }
}
