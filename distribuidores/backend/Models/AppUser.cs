namespace BackendDistribuidores.Models;

/// <summary>Usuario del portal (misma lógica que fábrica AppUser).</summary>
public class AppUser
{
    public long UserId { get; set; }
    public string Email { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string? FullName { get; set; }
    public string? Phone { get; set; }
    public string Status { get; set; } = "ACTIVE";
    public DateTime? CreatedAt { get; set; }

    /// <summary>Relación con roles (vía USER_ROLE). Cargar con Include(u => u.UserRoles).ThenInclude(ur => ur.Role).</summary>
    public ICollection<UserRole> UserRoles { get; set; } = new List<UserRole>();
}
