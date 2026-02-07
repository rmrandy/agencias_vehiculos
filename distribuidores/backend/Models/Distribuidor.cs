namespace BackendDistribuidores.Models;

public class Distribuidor
{
    public int Id { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public string? Contacto { get; set; }
    public string? Email { get; set; }
    public string? Telefono { get; set; }
}
