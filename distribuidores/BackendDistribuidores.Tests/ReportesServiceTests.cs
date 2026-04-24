using BackendDistribuidores.Models;
using BackendDistribuidores.Services;
using Xunit;

namespace BackendDistribuidores.Tests;

public class ReportesServiceTests
{
    [Fact]
    public async Task MasVendidos_excluye_cancelados_y_suma_local()
    {
        await using var db = TestAppDbContextFactory.Create();
        db.Categories.Add(new Category { Name = "C" });
        db.Brands.Add(new Brand { Name = "B" });
        await db.SaveChangesAsync();
        var part = new Part
        {
            CategoryId = db.Categories.First().CategoryId,
            BrandId = db.Brands.First().BrandId,
            PartNumber = "P-R1",
            Title = "Filtro",
            Price = 10m,
            StockQuantity = 100,
            ReservedQuantity = 0,
            Active = 1,
            CreatedAt = DateTime.UtcNow
        };
        db.Parts.Add(part);
        await db.SaveChangesAsync();

        var h1 = new OrderHeader
        {
            OrderNumber = "ORD-A",
            UserId = 1,
            Subtotal = 20m,
            ShippingTotal = 0,
            TariffTotal = 0,
            Total = 20m,
            CreatedAt = DateTime.UtcNow.Date
        };
        var h2 = new OrderHeader
        {
            OrderNumber = "ORD-B",
            UserId = 1,
            Subtotal = 10m,
            ShippingTotal = 0,
            TariffTotal = 0,
            Total = 10m,
            CreatedAt = DateTime.UtcNow.Date
        };
        db.OrderHeaders.AddRange(h1, h2);
        await db.SaveChangesAsync();

        db.OrderItems.Add(new OrderItem
        {
            OrderId = h1.OrderId,
            LineSource = "LOCAL",
            PartId = part.PartId,
            Qty = 2,
            UnitPrice = 10m,
            LineTotal = 20m
        });
        db.OrderItems.Add(new OrderItem
        {
            OrderId = h2.OrderId,
            LineSource = "LOCAL",
            PartId = part.PartId,
            Qty = 1,
            UnitPrice = 10m,
            LineTotal = 10m
        });
        db.OrderStatusHistories.Add(new OrderStatusHistory
        {
            OrderId = h1.OrderId,
            Status = "DELIVERED",
            ChangedByUserId = 1,
            ChangedAt = DateTime.UtcNow
        });
        db.OrderStatusHistories.Add(new OrderStatusHistory
        {
            OrderId = h2.OrderId,
            Status = "CANCELLED",
            ChangedByUserId = 1,
            ChangedAt = DateTime.UtcNow
        });
        await db.SaveChangesAsync();

        var svc = new ReportesService(db);
        var rows = await svc.GetMasVendidosAsync(DateTime.UtcNow.Date.AddDays(-1), DateTime.UtcNow.Date.AddDays(1), 10);

        Assert.Single(rows);
        Assert.Equal(part.PartId, rows[0].PartId);
        Assert.Equal(2, rows[0].TotalQty);
        Assert.Equal(20m, rows[0].TotalImporte);
    }
}
