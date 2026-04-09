using BackendDistribuidores.Models;
using Xunit;

namespace BackendDistribuidores.Tests;

public class PedidoItemRequestTests
{
    [Fact]
    public void IsFabricLine_sourceFabrica()
    {
        var i = new PedidoItemRequest { Source = "Fabrica", Qty = 1 };
        Assert.True(PedidoItemRequest.IsFabricLine(i));
    }

    [Fact]
    public void IsFabricLine_proveedorYPartId()
    {
        var i = new PedidoItemRequest { ProveedorId = 1, FabricaPartId = 2, Qty = 1 };
        Assert.True(PedidoItemRequest.IsFabricLine(i));
    }

    [Fact]
    public void IsFabricLine_local()
    {
        var i = new PedidoItemRequest { Source = "local", PartId = 5, Qty = 1 };
        Assert.False(PedidoItemRequest.IsFabricLine(i));
    }
}
