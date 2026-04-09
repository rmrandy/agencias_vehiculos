using BackendDistribuidores.Models;
using BackendDistribuidores.Services;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace BackendDistribuidores.Tests;

public class AuthServiceTests
{
    [Fact]
    public async Task RegisterAsync_passwordCorta_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var svc = new AuthService(db);
        var ex = await Assert.ThrowsAsync<ArgumentException>(() =>
            svc.RegisterAsync("a@b.com", "12345", null, null));
        Assert.Contains("6", ex.Message);
    }

    [Fact]
    public async Task RegisterAsync_emailVacio_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        var svc = new AuthService(db);
        await Assert.ThrowsAsync<ArgumentException>(() =>
            svc.RegisterAsync("  ", "123456", null, null));
    }

    [Fact]
    public async Task RegisterAsync_duplicado_lanza()
    {
        await using var db = TestAppDbContextFactory.Create();
        db.AppUsers.Add(new AppUser
        {
            Email = "dup@test.com",
            PasswordHash = "x",
            Status = "ACTIVE",
            CreatedAt = DateTime.UtcNow
        });
        await db.SaveChangesAsync();

        var svc = new AuthService(db);
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            svc.RegisterAsync("dup@test.com", "123456", null, null));
    }

    [Fact]
    public async Task RegisterAsync_yLogin_ok()
    {
        await using var db = TestAppDbContextFactory.Create();
        db.Roles.Add(new Role { Name = "USER" });
        await db.SaveChangesAsync();

        var svc = new AuthService(db);
        var user = await svc.RegisterAsync("ok@test.com", "secret12", "Nombre", null);
        Assert.True(user.UserId > 0);

        var logged = await svc.LoginAsync("ok@test.com", "secret12");
        Assert.NotNull(logged);
        Assert.Equal("ok@test.com", logged.Email);

        var fail = await svc.LoginAsync("ok@test.com", "mala");
        Assert.Null(fail);
    }

    [Fact]
    public async Task LoginAsync_credencialesVacias_null()
    {
        await using var db = TestAppDbContextFactory.Create();
        var svc = new AuthService(db);
        Assert.Null(await svc.LoginAsync("", "x"));
        Assert.Null(await svc.LoginAsync("a@b.com", ""));
    }
}
