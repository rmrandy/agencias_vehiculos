using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Autenticación local (misma lógica que fábrica: email + BCrypt).</summary>
public class AuthService
{
    private readonly AppDbContext _db;

    public AuthService(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Login con email y contraseña. Devuelve usuario con roles (nombres) o null.</summary>
    public async Task<AppUser?> LoginAsync(string email, string password, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrEmpty(password))
            return null;

        var user = await _db.AppUsers
            .Include(u => u.UserRoles)
            .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.Email == email.Trim() && u.Status == "ACTIVE", ct);

        if (user == null)
            return null;

        if (!BCrypt.Net.BCrypt.Verify(password, user.PasswordHash))
            return null;

        return user;
    }

    /// <summary>Registrar usuario (para portal local). Hash con BCrypt como fábrica.</summary>
    public async Task<AppUser> RegisterAsync(string email, string password, string? fullName, string? phone, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrEmpty(password))
            throw new ArgumentException("Email y contraseña son obligatorios");
        if (password.Length < 6)
            throw new ArgumentException("La contraseña debe tener al menos 6 caracteres");

        if (await _db.AppUsers.AnyAsync(u => u.Email == email.Trim(), ct))
            throw new InvalidOperationException("Ya existe un usuario con ese email");

        string hash = BCrypt.Net.BCrypt.HashPassword(password, BCrypt.Net.BCrypt.GenerateSalt(10));
        var user = new AppUser
        {
            Email = email.Trim(),
            PasswordHash = hash,
            FullName = fullName?.Trim(),
            Phone = phone?.Trim(),
            Status = "ACTIVE",
            CreatedAt = DateTime.UtcNow
        };
        _db.AppUsers.Add(user);
        await _db.SaveChangesAsync(ct);

        var roleUser = await _db.Roles.FirstOrDefaultAsync(r => r.Name == "USER", ct);
        if (roleUser != null)
        {
            _db.UserRoles.Add(new UserRole { UserId = user.UserId, RoleId = roleUser.RoleId });
            await _db.SaveChangesAsync(ct);
        }

        return user;
    }
}
