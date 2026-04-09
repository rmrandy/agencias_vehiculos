using BackendDistribuidores.Data;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Tests;

internal static class TestAppDbContextFactory
{
    public static AppDbContext Create(string? databaseName = null)
    {
        var name = databaseName ?? Guid.NewGuid().ToString("N");
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(name)
            .Options;
        return new AppDbContext(options);
    }
}
