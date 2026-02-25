using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Data;

public static class SeedData
{
    public static async Task EnsureSeedAsync(AppDbContext db, CancellationToken ct = default)
    {
        await SeedRolesAndUserAsync(db, ct);
        await SeedCatalogAsync(db, ct);
    }

    static async Task SeedRolesAndUserAsync(AppDbContext db, CancellationToken ct)
    {
        if (await db.Roles.AnyAsync(ct)) return;

        var roleUser = new Role { Name = "USER" };
        var roleAdmin = new Role { Name = "ADMIN" };
        var roleEmployee = new Role { Name = "EMPLOYEE" };
        db.Roles.Add(roleUser);
        db.Roles.Add(roleAdmin);
        db.Roles.Add(roleEmployee);
        await db.SaveChangesAsync(ct);

        if (await db.AppUsers.AnyAsync(ct)) return;

        string hash = BCrypt.Net.BCrypt.HashPassword("123456", BCrypt.Net.BCrypt.GenerateSalt(10));
        var user = new AppUser
        {
            Email = "admin@distribuidor.local",
            PasswordHash = hash,
            FullName = "Administrador",
            Status = "ACTIVE",
            CreatedAt = DateTime.UtcNow
        };
        db.AppUsers.Add(user);
        await db.SaveChangesAsync(ct);

        db.UserRoles.Add(new UserRole { UserId = user.UserId, RoleId = roleAdmin.RoleId });
        db.UserRoles.Add(new UserRole { UserId = user.UserId, RoleId = roleUser.RoleId });
        await db.SaveChangesAsync(ct);
    }

    /// <summary>Catálogo local: categorías, marcas y repuestos (misma lógica que fábrica datos dummy).</summary>
    static async Task SeedCatalogAsync(AppDbContext db, CancellationToken ct)
    {
        if (await db.Categories.AnyAsync(ct)) return;

        var catMotor = new Category { Name = "Motor", ParentId = null };
        var catTrans = new Category { Name = "Transmisión", ParentId = null };
        var catFrenos = new Category { Name = "Frenos", ParentId = null };
        var catSusp = new Category { Name = "Suspensión", ParentId = null };
        var catElec = new Category { Name = "Eléctrico", ParentId = null };
        var catFiltros = new Category { Name = "Filtros", ParentId = null };
        db.Categories.AddRange(catMotor, catTrans, catFrenos, catSusp, catElec, catFiltros);
        await db.SaveChangesAsync(ct);

        if (await db.Brands.AnyAsync(ct)) return;

        var brandBosch = new Brand { Name = "Bosch" };
        var brandDenso = new Brand { Name = "Denso" };
        var brandNGK = new Brand { Name = "NGK" };
        var brandBrembo = new Brand { Name = "Brembo" };
        var brandMann = new Brand { Name = "Mann Filter" };
        var brandMonroe = new Brand { Name = "Monroe" };
        db.Brands.AddRange(brandBosch, brandDenso, brandNGK, brandBrembo, brandMann, brandMonroe);
        await db.SaveChangesAsync(ct);

        if (await db.Parts.AnyAsync(ct)) return;

        var now = DateTime.UtcNow;
        db.Parts.AddRange(
            new Part { CategoryId = catFiltros.CategoryId, BrandId = brandMann.BrandId, PartNumber = "MF-OIL-001", Title = "Filtro de aceite Mann Filter", Description = "Filtro de aceite de alta eficiencia", WeightLb = 0.5m, Price = 15.99m, Active = 1, CreatedAt = now, StockQuantity = 50, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catFrenos.CategoryId, BrandId = brandBrembo.BrandId, PartNumber = "BRE-PAD-F200", Title = "Pastillas de freno delanteras Brembo", Description = "Pastillas cerámicas de alto rendimiento", WeightLb = 2.3m, Price = 89.99m, Active = 1, CreatedAt = now, StockQuantity = 30, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catElec.CategoryId, BrandId = brandNGK.BrandId, PartNumber = "NGK-SP-V4", Title = "Bujías NGK V-Power (set de 4)", Description = "Bujías de alto rendimiento", WeightLb = 0.8m, Price = 32.50m, Active = 1, CreatedAt = now, StockQuantity = 100, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catSusp.CategoryId, BrandId = brandMonroe.BrandId, PartNumber = "MON-SHOCK-58620", Title = "Amortiguador delantero Monroe", Description = "Amortiguador de gas de alta presión", WeightLb = 5.2m, Price = 125.00m, Active = 1, CreatedAt = now, StockQuantity = 20, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catElec.CategoryId, BrandId = brandBosch.BrandId, PartNumber = "BSH-ALT-12V-90A", Title = "Alternador Bosch 12V 90A", Description = "Alternador remanufacturado", WeightLb = 12.5m, Price = 245.00m, Active = 1, CreatedAt = now, StockQuantity = 15, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catElec.CategoryId, BrandId = brandDenso.BrandId, PartNumber = "DEN-O2-234-4668", Title = "Sensor de oxígeno Denso", Description = "Sensor O2 de 4 cables", WeightLb = 0.6m, Price = 67.50m, Active = 1, CreatedAt = now, StockQuantity = 40, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catFiltros.CategoryId, BrandId = brandMann.BrandId, PartNumber = "MF-AIR-C25114", Title = "Filtro de aire Mann Filter", Description = "Filtro de aire de papel", WeightLb = 0.9m, Price = 22.99m, Active = 1, CreatedAt = now, StockQuantity = 60, LowStockThreshold = 5, ReservedQuantity = 0 },
            new Part { CategoryId = catFrenos.CategoryId, BrandId = brandBrembo.BrandId, PartNumber = "BRE-DISC-09C84811", Title = "Discos de freno ventilados Brembo", Description = "Par de discos ventilados", WeightLb = 18.5m, Price = 189.99m, Active = 1, CreatedAt = now, StockQuantity = 25, LowStockThreshold = 5, ReservedQuantity = 0 }
        );
        await db.SaveChangesAsync(ct);
    }
}
