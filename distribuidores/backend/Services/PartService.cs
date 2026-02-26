using BackendDistribuidores.Data;
using BackendDistribuidores.Models;
using Microsoft.EntityFrameworkCore;

namespace BackendDistribuidores.Services;

/// <summary>Catálogo local y stock (misma lógica que fábrica: CRUD, imagen BLOB, reserva, confirmar venta).</summary>
public class PartService
{
    private const int MaxImageSizeBytes = 5 * 1024 * 1024; // 5MB como fábrica
    private static readonly string[] ValidImageTypes = { "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp" };

    private readonly AppDbContext _db;

    public PartService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<Part?> GetByIdAsync(long partId, CancellationToken ct = default)
    {
        return await _db.Parts.FindAsync(new object[] { partId }, ct);
    }

    public async Task<Part?> GetByPartNumberAsync(string partNumber, CancellationToken ct = default)
    {
        return await _db.Parts.FirstOrDefaultAsync(p => p.PartNumber == partNumber, ct);
    }

    public async Task<List<Part>> ListAsync(long? categoryId, long? brandId, bool includeInactive = false, CancellationToken ct = default)
    {
        var q = includeInactive ? _db.Parts.AsQueryable() : _db.Parts.Where(p => p.Active != 0);
        if (categoryId.HasValue) q = q.Where(p => p.CategoryId == categoryId.Value);
        if (brandId.HasValue) q = q.Where(p => p.BrandId == brandId.Value);
        return await q.OrderBy(p => p.Title).ToListAsync(ct);
    }

    public async Task<List<Part>> SearchAsync(string? query, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(query))
            return await _db.Parts.Where(p => p.Active != 0).OrderBy(p => p.Title).ToListAsync(ct);

        var term = query.Trim().ToLower();
        return await _db.Parts
            .Where(p => p.Active != 0 && (
                p.Title.ToLower().Contains(term) ||
                (p.Description != null && p.Description.ToLower().Contains(term)) ||
                p.PartNumber.ToLower().Contains(term)))
            .OrderBy(p => p.Title)
            .ToListAsync(ct);
    }

    /// <summary>Crear repuesto (misma lógica que fábrica). Imagen se asigna después con UpdateImageAsync.</summary>
    public async Task<Part> CreateAsync(long categoryId, long brandId, string partNumber, string title,
        string? description, decimal? weightLb, decimal price, int stockQuantity = 0, int lowStockThreshold = 5,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(partNumber)) throw new ArgumentException("El número de parte es obligatorio");
        if (string.IsNullOrWhiteSpace(title)) throw new ArgumentException("El título es obligatorio");
        if (price < 0) throw new ArgumentException("El precio debe ser mayor o igual a cero");
        if (await GetByPartNumberAsync(partNumber.Trim(), ct) != null)
            throw new ArgumentException("Ya existe un repuesto con ese número de parte");

        var part = new Part
        {
            CategoryId = categoryId,
            BrandId = brandId,
            PartNumber = partNumber.Trim(),
            Title = title.Trim(),
            Description = description?.Trim(),
            WeightLb = weightLb,
            Price = price,
            Active = 1,
            CreatedAt = DateTime.UtcNow,
            StockQuantity = stockQuantity,
            LowStockThreshold = lowStockThreshold,
            ReservedQuantity = 0
        };
        _db.Parts.Add(part);
        await _db.SaveChangesAsync(ct);
        return part;
    }

    /// <summary>Actualizar datos básicos (misma lógica que fábrica).</summary>
    public async Task<Part> UpdateAsync(long id, long? categoryId, long? brandId, string? title, string? description,
        decimal? weightLb, decimal? price, int? active, CancellationToken ct = default)
    {
        var part = await GetByIdAsync(id, ct) ?? throw new ArgumentException("Repuesto no encontrado");
        if (categoryId.HasValue) part.CategoryId = categoryId.Value;
        if (brandId.HasValue) part.BrandId = brandId.Value;
        if (!string.IsNullOrWhiteSpace(title)) part.Title = title.Trim();
        if (description != null) part.Description = description.Trim();
        if (weightLb.HasValue) part.WeightLb = weightLb;
        if (price.HasValue) part.Price = price.Value;
        if (active.HasValue) part.Active = active.Value;
        await _db.SaveChangesAsync(ct);
        return part;
    }

    /// <summary>Actualizar imagen BLOB (misma lógica que fábrica).</summary>
    public async Task<Part> UpdateImageAsync(long id, byte[] imageData, string? imageType, CancellationToken ct = default)
    {
        if (imageData.Length > MaxImageSizeBytes)
            throw new ArgumentException("La imagen excede el tamaño máximo de 5MB");
        if (!string.IsNullOrEmpty(imageType) && !ValidImageTypes.Contains(imageType.Trim().ToLowerInvariant()))
            throw new ArgumentException("Formato de imagen no válido. Use: image/jpeg, image/png, image/gif, image/webp");

        var part = await GetByIdAsync(id, ct) ?? throw new ArgumentException("Repuesto no encontrado");
        part.ImageData = imageData;
        part.ImageType = imageType?.Trim();
        await _db.SaveChangesAsync(ct);
        return part;
    }

    /// <summary>Decodificar base64 (acepta data:image/...;base64,) y validar tamaño/tipo.</summary>
    public static (byte[] data, string? type) DecodeImageBase64(string? base64Data, string? imageType)
    {
        if (string.IsNullOrWhiteSpace(base64Data)) return (Array.Empty<byte>(), imageType);
        var data = base64Data.Trim();
        if (data.Contains(',')) data = data.Split(',', 2)[1].Trim();
        byte[] bytes;
        try
        {
            bytes = Convert.FromBase64String(data);
        }
        catch (FormatException)
        {
            throw new ArgumentException("Imagen inválida: formato base64 incorrecto");
        }
        if (bytes.Length > MaxImageSizeBytes)
            throw new ArgumentException("La imagen excede el tamaño máximo de 5MB");
        if (!string.IsNullOrEmpty(imageType) && !ValidImageTypes.Contains(imageType.Trim().ToLowerInvariant()))
            throw new ArgumentException("Formato de imagen no válido");
        return (bytes, imageType);
    }

    public async Task<Part> UpdateInventoryAsync(long id, int? stockQuantity, int? lowStockThreshold, CancellationToken ct = default)
    {
        var part = await GetByIdAsync(id, ct) ?? throw new ArgumentException("Repuesto no encontrado");
        if (stockQuantity.HasValue && stockQuantity.Value >= 0) part.StockQuantity = stockQuantity.Value;
        if (lowStockThreshold.HasValue && lowStockThreshold.Value >= 0) part.LowStockThreshold = lowStockThreshold.Value;
        await _db.SaveChangesAsync(ct);
        return part;
    }

    public async Task DeleteAsync(long id, CancellationToken ct = default)
    {
        var part = await GetByIdAsync(id, ct);
        if (part == null) return;
        var extras = await _db.PartImages.Where(pi => pi.PartId == id).ToListAsync(ct);
        _db.PartImages.RemoveRange(extras);
        _db.Parts.Remove(part);
        await _db.SaveChangesAsync(ct);
    }

    /// <summary>Cantidad total de imágenes (principal + galería). No carga el BLOB para evitar 500/timeouts.</summary>
    public async Task<int> GetGalleryCountAsync(long partId, CancellationToken ct = default)
    {
        try
        {
            var partExists = await _db.Parts.AnyAsync(p => p.PartId == partId, ct);
            if (!partExists) return 0;
            int count = 0;
            try
            {
                var hasMain = await _db.Database
                    .SqlQueryRaw<int>("SELECT CASE WHEN ImageData IS NOT NULL AND DATALENGTH(ImageData) > 0 THEN 1 ELSE 0 END AS [Value] FROM PART WHERE PartId = {0}", partId)
                    .FirstOrDefaultAsync(ct);
                if (hasMain > 0) count++;
            }
            catch
            {
                // Columna ImageData puede no existir en PART
            }
            try
            {
                count += await _db.PartImages.CountAsync(pi => pi.PartId == partId, ct);
            }
            catch
            {
                // Tabla PART_IMAGE puede no existir si la BD se creó antes de añadir galería
            }
            return count;
        }
        catch (Exception ex)
        {
            Console.WriteLine("[PartService] GetGalleryCountAsync error for part {0}: {1}", partId, ex.Message);
            return 0;
        }
    }

    /// <summary>Obtiene imagen por índice: 0 = principal (Part), 1+ = PartImages por SortOrder.</summary>
    public async Task<(byte[] data, string? type)?> GetPartImageByIndexAsync(long partId, int index, CancellationToken ct = default)
    {
        var part = await GetByIdAsync(partId, ct);
        if (part == null) return null;
        if (index == 0 && part.ImageData != null && part.ImageData.Length > 0)
            return (part.ImageData, part.ImageType);
        if (index < 0) return null;
        var extra = await _db.PartImages
            .Where(pi => pi.PartId == partId)
            .OrderBy(pi => pi.SortOrder)
            .Skip(index - 1)
            .Take(1)
            .FirstOrDefaultAsync(ct);
        if (extra == null) return null;
        return (extra.ImageData, extra.ImageType);
    }

    /// <summary>Agrega una imagen a la galería del producto.</summary>
    public async Task AddPartImageAsync(long partId, byte[] imageData, string? imageType, CancellationToken ct = default)
    {
        if (imageData.Length > MaxImageSizeBytes)
            throw new ArgumentException("La imagen excede el tamaño máximo de 5MB");
        if (!string.IsNullOrEmpty(imageType) && !ValidImageTypes.Contains(imageType.Trim().ToLowerInvariant()))
            throw new ArgumentException("Formato de imagen no válido");
        var part = await GetByIdAsync(partId, ct) ?? throw new ArgumentException("Repuesto no encontrado");
        int nextOrder = await _db.PartImages.Where(pi => pi.PartId == partId).CountAsync(ct);
        _db.PartImages.Add(new PartImage
        {
            PartId = partId,
            ImageData = imageData,
            ImageType = imageType,
            SortOrder = nextOrder
        });
        await _db.SaveChangesAsync(ct);
    }

    public bool CheckAvailability(long partId, int qty)
    {
        var part = _db.Parts.Find(partId);
        if (part == null) return false;
        int available = part.StockQuantity - part.ReservedQuantity;
        return available >= qty;
    }

    public bool ReserveStock(long partId, int qty)
    {
        var part = _db.Parts.Find(partId);
        if (part == null) return false;
        int available = part.StockQuantity - part.ReservedQuantity;
        if (available < qty) return false;
        part.ReservedQuantity += qty;
        _db.SaveChanges();
        return true;
    }

    public void ReleaseStock(long partId, int qty)
    {
        var part = _db.Parts.Find(partId);
        if (part == null) return;
        part.ReservedQuantity = Math.Max(0, part.ReservedQuantity - qty);
        _db.SaveChanges();
    }

    public void ConfirmSale(long partId, int qty)
    {
        var part = _db.Parts.Find(partId);
        if (part == null) return;
        part.StockQuantity = Math.Max(0, part.StockQuantity - qty);
        part.ReservedQuantity = Math.Max(0, part.ReservedQuantity - qty);
        _db.SaveChanges();
    }
}
