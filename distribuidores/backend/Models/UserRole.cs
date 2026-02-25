namespace BackendDistribuidores.Models;

/// <summary>Relación usuario-rol (tabla USER_ROLE como en fábrica).</summary>
public class UserRole
{
    public long UserId { get; set; }
    public long RoleId { get; set; }
    public AppUser User { get; set; } = null!;
    public Role Role { get; set; } = null!;
}
