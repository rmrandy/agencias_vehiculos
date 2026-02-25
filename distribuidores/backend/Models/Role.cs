namespace BackendDistribuidores.Models;

/// <summary>Rol de usuario (misma lógica que fábrica).</summary>
public class Role
{
    public long RoleId { get; set; }
    public string Name { get; set; } = string.Empty;

    public ICollection<UserRole> UserRoles { get; set; } = new List<UserRole>();
}
