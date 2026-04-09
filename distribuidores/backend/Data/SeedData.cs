using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Data;

public static class SeedData
{
    public static async Task EnsureSeedAsync(AppDbContext db, CancellationToken ct = default)
    {
        await EnsureArancelSchemaAsync(db, ct);
        await EnsureMonedaSchemaAsync(db, ct);
        await EnsureEnvioConfigSchemaAsync(db, ct);
        await EnsurePartImageTableAsync(db, ct);
        await EnsureMultisourceSchemaAsync(db, ct);
        await SeedArancelPaisesAsync(db, ct);
        await SeedMonedasAsync(db, ct);
        await SeedEnvioConfigAsync(db, ct);
        await SeedRolesAndUserAsync(db, ct);
        await SeedCatalogAsync(db, ct);
    }

    /// <summary>Tabla ARANCEL_PAIS y columnas de arancel en ORDER_HEADER (BD existente).</summary>
    static async Task EnsureArancelSchemaAsync(AppDbContext db, CancellationToken ct)
    {
        try
        {
            await db.Database.ExecuteSqlRawAsync(
                @"IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ARANCEL_PAIS')
                  CREATE TABLE ARANCEL_PAIS (
                    CountryCode nvarchar(2) NOT NULL PRIMARY KEY,
                    CountryName nvarchar(80) NOT NULL,
                    TariffPercent decimal(7,4) NOT NULL CONSTRAINT DF_ARANCEL_Tariff DEFAULT 0
                  );
                IF COL_LENGTH(N'ORDER_HEADER', N'TariffTotal') IS NULL
                  ALTER TABLE ORDER_HEADER ADD TariffTotal decimal(12,2) NOT NULL CONSTRAINT DF_ORDER_HEADER_TariffTotal DEFAULT 0;
                IF COL_LENGTH(N'ORDER_HEADER', N'ShippingCountryCode') IS NULL
                  ALTER TABLE ORDER_HEADER ADD ShippingCountryCode nvarchar(2) NULL;", ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] EnsureArancelSchema: " + ex.Message);
        }
    }

    /// <summary>Tabla MONEDA (tipo de cambio por 1 USD).</summary>
    static async Task EnsureMonedaSchemaAsync(AppDbContext db, CancellationToken ct)
    {
        try
        {
            await db.Database.ExecuteSqlRawAsync(
                @"IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MONEDA')
                  CREATE TABLE MONEDA (
                    Code nvarchar(3) NOT NULL PRIMARY KEY,
                    Name nvarchar(80) NOT NULL,
                    Symbol nvarchar(8) NOT NULL,
                    UnitsPerUsd decimal(14,6) NOT NULL CONSTRAINT DF_MONEDA_Units DEFAULT 1,
                    Activo bit NOT NULL CONSTRAINT DF_MONEDA_Activo DEFAULT 1,
                    SortOrder int NOT NULL CONSTRAINT DF_MONEDA_Sort DEFAULT 0
                  );", ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] EnsureMonedaSchema: " + ex.Message);
        }
    }

    static async Task SeedMonedasAsync(AppDbContext db, CancellationToken ct)
    {
        var seed = new[]
        {
            new Moneda { Code = "USD", Name = "Dólar estadounidense", Symbol = "US$", UnitsPerUsd = 1m, Activo = true, SortOrder = 0 },
            new Moneda { Code = "GTQ", Name = "Quetzal guatemalteco", Symbol = "Q", UnitsPerUsd = 7.85m, Activo = true, SortOrder = 1 },
            new Moneda { Code = "MXN", Name = "Peso mexicano", Symbol = "MX$", UnitsPerUsd = 17.20m, Activo = true, SortOrder = 2 },
            new Moneda { Code = "EUR", Name = "Euro", Symbol = "€", UnitsPerUsd = 0.92m, Activo = true, SortOrder = 3 },
        };

        foreach (var m in seed)
        {
            if (await db.Monedas.AnyAsync(x => x.Code == m.Code, ct))
                continue;
            try
            {
                db.Monedas.Add(m);
                await db.SaveChangesAsync(ct);
            }
            catch (Exception ex)
            {
                Console.WriteLine("[SeedData] SeedMoneda " + m.Code + ": " + ex.Message);
            }
        }
    }

    /// <summary>Tabla ENVIO_CONFIG y fila por defecto (BD existente sin migraciones).</summary>
    static async Task EnsureEnvioConfigSchemaAsync(AppDbContext db, CancellationToken ct)
    {
        try
        {
            await db.Database.ExecuteSqlRawAsync(
                @"IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ENVIO_CONFIG')
                  CREATE TABLE ENVIO_CONFIG (
                    Id int NOT NULL PRIMARY KEY,
                    UsdPerLb decimal(12,4) NOT NULL CONSTRAINT DF_ENVIO_UsdPerLb DEFAULT 0
                  );
                IF NOT EXISTS (SELECT 1 FROM ENVIO_CONFIG WHERE Id = 1)
                  INSERT INTO ENVIO_CONFIG (Id, UsdPerLb) VALUES (1, 0);", ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] EnsureEnvioConfigSchema: " + ex.Message);
        }
    }

    static async Task SeedEnvioConfigAsync(AppDbContext db, CancellationToken ct)
    {
        if (await db.EnvioConfigs.AnyAsync(e => e.Id == EnvioConfig.SingletonId, ct))
            return;
        try
        {
            db.EnvioConfigs.Add(new EnvioConfig { Id = EnvioConfig.SingletonId, UsdPerLb = 0 });
            await db.SaveChangesAsync(ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] SeedEnvioConfig: " + ex.Message);
        }
    }

    static async Task SeedArancelPaisesAsync(AppDbContext db, CancellationToken ct)
    {
        foreach (var kv in LatamCountries.All)
        {
            if (await db.ArancelPaises.AnyAsync(a => a.CountryCode == kv.Key, ct))
                continue;
            db.ArancelPaises.Add(new ArancelPais
            {
                CountryCode = kv.Key,
                CountryName = kv.Value,
                TariffPercent = 0
            });
        }

        try
        {
            await db.SaveChangesAsync(ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] SeedArancelPaises: " + ex.Message);
        }
    }

    /// <summary>Columnas para pedidos multi-fábrica y usuario de integración en PROVEEDOR (BD creada antes de este cambio).</summary>
    static async Task EnsureMultisourceSchemaAsync(AppDbContext db, CancellationToken ct)
    {
        try
        {
            await db.Database.ExecuteSqlRawAsync(
                @"IF COL_LENGTH(N'ORDER_ITEM', N'LineSource') IS NULL
                  ALTER TABLE ORDER_ITEM ADD LineSource nvarchar(20) NOT NULL CONSTRAINT DF_ORDER_ITEM_LineSource DEFAULT N'LOCAL';
                IF COL_LENGTH(N'ORDER_ITEM', N'ProveedorId') IS NULL
                  ALTER TABLE ORDER_ITEM ADD ProveedorId bigint NULL;
                IF COL_LENGTH(N'ORDER_ITEM', N'FabricaPartId') IS NULL
                  ALTER TABLE ORDER_ITEM ADD FabricaPartId bigint NULL;
                IF COL_LENGTH(N'ORDER_ITEM', N'FabricaOrderId') IS NULL
                  ALTER TABLE ORDER_ITEM ADD FabricaOrderId bigint NULL;
                IF COL_LENGTH(N'ORDER_ITEM', N'TitleSnapshot') IS NULL
                  ALTER TABLE ORDER_ITEM ADD TitleSnapshot nvarchar(500) NULL;
                IF COL_LENGTH(N'ORDER_ITEM', N'PartNumberSnapshot') IS NULL
                  ALTER TABLE ORDER_ITEM ADD PartNumberSnapshot nvarchar(100) NULL;
                IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'ORDER_ITEM') AND name = N'PartId' AND is_nullable = 0)
                  ALTER TABLE ORDER_ITEM ALTER COLUMN PartId bigint NULL;
                IF COL_LENGTH(N'PROVEEDOR', N'FabricaEnterpriseUserId') IS NULL
                  ALTER TABLE PROVEEDOR ADD FabricaEnterpriseUserId bigint NULL;", ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] EnsureMultisourceSchema: " + ex.Message);
        }
    }

    /// <summary>Crea la tabla PART_IMAGE si no existe (por si la BD se creó antes de añadir galería).</summary>
    static async Task EnsurePartImageTableAsync(AppDbContext db, CancellationToken ct)
    {
        try
        {
            await db.Database.ExecuteSqlRawAsync(
                @"IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PART_IMAGE')
                  CREATE TABLE PART_IMAGE (
                    PartImageId bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    PartId bigint NOT NULL,
                    SortOrder int NOT NULL,
                    ImageData varbinary(max) NOT NULL,
                    ImageType nvarchar(50) NULL
                  );", ct);
        }
        catch (Exception ex)
        {
            Console.WriteLine("[SeedData] EnsurePartImageTable: " + ex.Message);
        }
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
