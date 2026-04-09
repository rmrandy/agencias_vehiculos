using BackendDistribuidores.Services;
using Xunit;

namespace BackendDistribuidores.Tests;

public class PartServiceDecodeImageTests
{
    [Fact]
    public void DecodeImageBase64_vacio()
    {
        var (data, type) = PartService.DecodeImageBase64(null, "image/png");
        Assert.Empty(data);
    }

    [Fact]
    public void DecodeImageBase64_dataUri()
    {
        var raw = Convert.ToBase64String(new byte[] { 1, 2, 3 });
        var (data, _) = PartService.DecodeImageBase64("data:image/png;base64," + raw, "image/png");
        Assert.Equal(new byte[] { 1, 2, 3 }, data);
    }

    [Fact]
    public void DecodeImageBase64_base64Invalido_lanza()
    {
        Assert.Throws<ArgumentException>(() => PartService.DecodeImageBase64("!!!", "image/png"));
    }

    [Fact]
    public void DecodeImageBase64_tipoInvalido_lanza()
    {
        var b64 = Convert.ToBase64String(new byte[] { 1 });
        Assert.Throws<ArgumentException>(() => PartService.DecodeImageBase64(b64, "text/plain"));
    }
}
