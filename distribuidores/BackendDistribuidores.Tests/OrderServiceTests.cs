using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using BackendDistribuidores.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace BackendDistribuidores.Tests;

public class OrderServiceTests
{
    private static FabricaIntegrationService CreateFabricaService()
    {
        var factory = new Mock<IHttpClientFactory>();
        factory.Setup(f => f.CreateClient(It.IsAny<string>())).Returns(new HttpClient());
        var config = new Mock<IConfiguration>();
        config.Setup(c => c["Integration:DistributorApiKey"]).Returns((string?)null);
        return new FabricaIntegrationService(factory.Object, config.Object);
    }

    private static async Task<(AppDbContext db, Part part)> SeedPartWithStockAsync()
    {
        var db = TestAppDbContextFactory.Create();
        db.Categories.Add(new Category { Name = "Cat" });
        db.Brands.Add(new Brand { Name = "Br" });
        await db.SaveChangesAsync();

        var part = new Part
        {
            CategoryId = db.Categories.First().CategoryId,
            BrandId = db.Brands.First().BrandId,
            PartNumber = "PN-1",
            Title = "Repuesto",
            Price = 25m,
            StockQuantity = 10,
            ReservedQuantity = 0,
            Active = 1,
            CreatedAt = DateTime.UtcNow
        };
        db.Parts.Add(part);
        await db.SaveChangesAsync();
        return (db, part);
    }

    [Fact]
    public async Task CreateOrderAsync_sinItems_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var partSvc = new PartService(db);
        var orderSvc = new OrderService(db, partSvc, CreateFabricaService());

        await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateOrderAsync(1, new List<OrderItemDto>(), CancellationToken.None));
    }

    [Fact]
    public async Task CreateOrderAsync_parteInexistente_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var partSvc = new PartService(db);
        var orderSvc = new OrderService(db, partSvc, CreateFabricaService());

        await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateOrderAsync(1, new List<OrderItemDto> { new() { PartId = 999, Qty = 1 } },
                CancellationToken.None));
    }

    [Fact]
    public async Task CreateOrderAsync_exito()
    {
        var (db, part) = await SeedPartWithStockAsync();
        await using (db)
        {
            var partSvc = new PartService(db);
            var orderSvc = new OrderService(db, partSvc, CreateFabricaService());

            var header = await orderSvc.CreateOrderAsync(1,
                new List<OrderItemDto> { new() { PartId = part.PartId, Qty = 2 } },
                CancellationToken.None);

            Assert.False(string.IsNullOrEmpty(header.OrderNumber));
            Assert.Equal(50m, header.Total);

            var refreshed = await db.Parts.FindAsync(part.PartId);
            Assert.NotNull(refreshed);
            Assert.Equal(8, refreshed.StockQuantity);
            Assert.Equal(0, refreshed.ReservedQuantity);

            var items = await db.OrderItems.Where(i => i.OrderId == header.OrderId).ToListAsync();
            Assert.Single(items);
            Assert.Equal(2, items[0].Qty);
        }
    }

    [Fact]
    public async Task CreateMultiSourceOrderAsync_vacio_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var partSvc = new PartService(db);
        var orderSvc = new OrderService(db, partSvc, CreateFabricaService());

        await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateMultiSourceOrderAsync(1, new List<PedidoItemRequest>(), null,
                CancellationToken.None));
    }

    [Fact]
    public async Task CreateMultiSourceOrderAsync_localCantidadInvalida_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var orderSvc = new OrderService(db, new PartService(db), CreateFabricaService());

        var items = new List<PedidoItemRequest>
        {
            new() { PartId = 1, Qty = 0 }
        };

        await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateMultiSourceOrderAsync(1, items, null, CancellationToken.None));
    }

    [Fact]
    public async Task CreateMultiSourceOrderAsync_fabricaSinPrecio_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var orderSvc = new OrderService(db, new PartService(db), CreateFabricaService());

        var items = new List<PedidoItemRequest>
        {
            new()
            {
                Source = "fabrica",
                ProveedorId = 1,
                FabricaPartId = 10,
                Qty = 1,
                UnitPrice = null
            }
        };

        await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateMultiSourceOrderAsync(1, items, null, CancellationToken.None));
    }

    [Fact]
    public async Task CreateMultiSourceOrderAsync_fabricaProveedorSinUrl_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        db.Proveedores.Add(new Proveedor
        {
            Nombre = "P1",
            Activo = true,
            ApiBaseUrl = "",
            FabricaEnterpriseUserId = 1
        });
        await db.SaveChangesAsync();
        var provId = db.Proveedores.First().ProveedorId;

        var orderSvc = new OrderService(db, new PartService(db), CreateFabricaService());
        var items = new List<PedidoItemRequest>
        {
            new()
            {
                Source = "fabrica",
                ProveedorId = provId,
                FabricaPartId = 99,
                Qty = 1,
                UnitPrice = 5m
            }
        };

        var ex = await Assert.ThrowsAsync<ArgumentException>(() =>
            orderSvc.CreateMultiSourceOrderAsync(1, items, null, CancellationToken.None));
        Assert.Contains("apiBaseUrl", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateMultiSourceOrderAsync_soloLocal_exito()
    {
        var (db, part) = await SeedPartWithStockAsync();
        await using (db)
        {
            var partSvc = new PartService(db);
            var orderSvc = new OrderService(db, partSvc, CreateFabricaService());

            var items = new List<PedidoItemRequest>
            {
                new() { Source = "local", PartId = part.PartId, Qty = 1 }
            };

            var header = await orderSvc.CreateMultiSourceOrderAsync(2, items, null, CancellationToken.None);
            Assert.Equal(25m, header.Total);
            var line = await db.OrderItems.FirstAsync(i => i.OrderId == header.OrderId);
            Assert.Equal("LOCAL", line.LineSource);
        }
    }
}
