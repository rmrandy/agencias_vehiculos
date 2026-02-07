using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<Distribuidor> Distribuidores => Set<Distribuidor>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Distribuidor>(e =>
        {
            e.ToTable("Distribuidores");
            e.HasKey(x => x.Id);
            e.Property(x => x.Nombre).HasMaxLength(200).IsRequired();
            e.Property(x => x.Contacto).HasMaxLength(200);
            e.Property(x => x.Email).HasMaxLength(200);
            e.Property(x => x.Telefono).HasMaxLength(50);
        });
    }
}
