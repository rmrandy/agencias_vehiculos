using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Data;

/// <summary>
/// Base de datos de la distribuidora (SQL Server). Misma estructura lógica que la fábrica:
/// catálogo (Category, Brand, Part), usuarios (AppUser, Role), pedidos (OrderHeader, OrderItem, OrderStatusHistory),
/// comentarios (PartReview), proveedores (Proveedor). Tabla Distribuidores se mantiene por compatibilidad.
/// </summary>
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<Distribuidor> Distribuidores => Set<Distribuidor>();
    public DbSet<Role> Roles => Set<Role>();
    public DbSet<AppUser> AppUsers => Set<AppUser>();
    public DbSet<UserRole> UserRoles => Set<UserRole>();
    public DbSet<Category> Categories => Set<Category>();
    public DbSet<Brand> Brands => Set<Brand>();
    public DbSet<Part> Parts => Set<Part>();
    public DbSet<PartImage> PartImages => Set<PartImage>();
    public DbSet<OrderHeader> OrderHeaders => Set<OrderHeader>();
    public DbSet<OrderItem> OrderItems => Set<OrderItem>();
    public DbSet<OrderStatusHistory> OrderStatusHistories => Set<OrderStatusHistory>();
    public DbSet<PartReview> PartReviews => Set<PartReview>();
    public DbSet<Proveedor> Proveedores => Set<Proveedor>();

    protected override void OnModelCreating(ModelBuilder mb)
    {
        // Distribuidores (existente)
        mb.Entity<Distribuidor>(e =>
        {
            e.ToTable("Distribuidores");
            e.HasKey(x => x.Id);
            e.Property(x => x.Nombre).HasMaxLength(200).IsRequired();
            e.Property(x => x.Contacto).HasMaxLength(200);
            e.Property(x => x.Email).HasMaxLength(200);
            e.Property(x => x.Telefono).HasMaxLength(50);
        });

        // ROLE (como fábrica)
        mb.Entity<Role>(e =>
        {
            e.ToTable("ROLE");
            e.HasKey(x => x.RoleId);
            e.Property(x => x.Name).HasMaxLength(100).IsRequired();
            e.HasMany(x => x.UserRoles).WithOne(ur => ur.Role).HasForeignKey(ur => ur.RoleId);
        });

        // APP_USER (como fábrica)
        mb.Entity<AppUser>(e =>
        {
            e.ToTable("APP_USER");
            e.HasKey(x => x.UserId);
            e.Property(x => x.Email).HasMaxLength(255).IsRequired();
            e.Property(x => x.PasswordHash).HasMaxLength(255).IsRequired();
            e.Property(x => x.FullName).HasMaxLength(200);
            e.Property(x => x.Phone).HasMaxLength(50);
            e.Property(x => x.Status).HasMaxLength(20);
            e.HasMany(x => x.UserRoles).WithOne(ur => ur.User).HasForeignKey(ur => ur.UserId);
        });

        mb.Entity<UserRole>(e =>
        {
            e.ToTable("USER_ROLE");
            e.HasKey(ur => new { ur.UserId, ur.RoleId });
        });

        // CATEGORY
        mb.Entity<Category>(e =>
        {
            e.ToTable("CATEGORY");
            e.HasKey(x => x.CategoryId);
            e.Property(x => x.Name).HasMaxLength(200).IsRequired();
            e.Property(x => x.ImageType).HasMaxLength(50);
        });

        // BRAND
        mb.Entity<Brand>(e =>
        {
            e.ToTable("BRAND");
            e.HasKey(x => x.BrandId);
            e.Property(x => x.Name).HasMaxLength(200).IsRequired();
            e.Property(x => x.ImageType).HasMaxLength(50);
        });

        // PART
        mb.Entity<Part>(e =>
        {
            e.ToTable("PART");
            e.HasKey(x => x.PartId);
            e.Property(x => x.PartNumber).HasMaxLength(100).IsRequired();
            e.Property(x => x.Title).HasMaxLength(500).IsRequired();
            e.Property(x => x.ImageType).HasMaxLength(50);
            e.Property(x => x.Price).HasPrecision(12, 2);
            e.Property(x => x.WeightLb).HasPrecision(10, 2);
        });

        // ORDER_HEADER
        mb.Entity<OrderHeader>(e =>
        {
            e.ToTable("ORDER_HEADER");
            e.HasKey(x => x.OrderId);
            e.Property(x => x.OrderNumber).HasMaxLength(50).IsRequired();
            e.Property(x => x.OrderType).HasMaxLength(20);
            e.Property(x => x.Currency).HasMaxLength(3);
            e.Property(x => x.Subtotal).HasPrecision(12, 2);
            e.Property(x => x.ShippingTotal).HasPrecision(12, 2);
            e.Property(x => x.Total).HasPrecision(12, 2);
        });

        // ORDER_ITEM
        mb.Entity<OrderItem>(e =>
        {
            e.ToTable("ORDER_ITEM");
            e.HasKey(x => x.OrderItemId);
            e.Property(x => x.UnitPrice).HasPrecision(12, 2);
            e.Property(x => x.LineTotal).HasPrecision(12, 2);
        });

        // ORDER_STATUS_HISTORY
        mb.Entity<OrderStatusHistory>(e =>
        {
            e.ToTable("ORDER_STATUS_HISTORY");
            e.HasKey(x => x.StatusId);
            e.Property(x => x.Status).HasMaxLength(30).IsRequired();
            e.Property(x => x.CommentText).HasMaxLength(500);
            e.Property(x => x.TrackingNumber).HasMaxLength(100);
        });

        // PART_IMAGE (galería: varias fotos por producto)
        mb.Entity<PartImage>(e =>
        {
            e.ToTable("PART_IMAGE");
            e.HasKey(x => x.PartImageId);
        });

        // PART_REVIEW
        mb.Entity<PartReview>(e =>
        {
            e.ToTable("PART_REVIEW");
            e.HasKey(x => x.ReviewId);
            e.Property(x => x.Body).HasMaxLength(2000);
        });

        // PROVEEDOR (DOC2)
        mb.Entity<Proveedor>(e =>
        {
            e.ToTable("PROVEEDOR");
            e.HasKey(x => x.ProveedorId);
            e.Property(x => x.Nombre).HasMaxLength(200).IsRequired();
            e.Property(x => x.Contacto).HasMaxLength(200);
            e.Property(x => x.Email).HasMaxLength(200);
            e.Property(x => x.Telefono).HasMaxLength(50);
            e.Property(x => x.ApiBaseUrl).HasMaxLength(500);
        });
    }
}
