using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/repuestos/{partId:long}/comentarios")]
public class ComentariosController : ControllerBase
{
    private readonly AppDbContext _db;

    public ComentariosController(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Comentarios recursivos (árbol por parentId). Raíz: parentId null; respuestas: parentId = reviewId del padre.</summary>
    [HttpGet]
    public async Task<IActionResult> GetTree(long partId, CancellationToken ct)
    {
        var list = await _db.PartReviews
            .Where(r => r.PartId == partId)
            .OrderBy(r => r.CreatedAt)
            .ToListAsync(ct);
        var users = await _db.AppUsers
            .Where(u => list.Select(x => x.UserId).Distinct().Contains(u.UserId))
            .ToDictionaryAsync(u => u.UserId, ct);
        var dtos = list.Select(r => new ComentarioDto
        {
            reviewId = r.ReviewId,
            partId = r.PartId,
            userId = r.UserId,
            userEmail = users.GetValueOrDefault(r.UserId)?.Email,
            fullName = users.GetValueOrDefault(r.UserId)?.FullName,
            parentId = r.ParentId,
            rating = r.Rating,
            body = r.Body,
            createdAt = r.CreatedAt
        }).ToList();
        var tree = BuildTree(dtos, null);
        return Ok(tree);
    }

    [HttpPost]
    public async Task<IActionResult> Create(long partId, [FromBody] CreateComentarioRequest body, CancellationToken ct)
    {
        if (body?.UserId == null)
            return BadRequest(new { message = "userId es obligatorio" });
        if (string.IsNullOrWhiteSpace(body.Body))
            return BadRequest(new { message = "El comentario no puede estar vacío" });
        var part = await _db.Parts.FindAsync(new object[] { partId }, ct);
        if (part == null) return NotFound(new { message = "Repuesto no encontrado" });
        if (body.ParentId.HasValue)
        {
            var parent = await _db.PartReviews.FindAsync(new object[] { body.ParentId.Value }, ct);
            if (parent == null || parent.PartId != partId)
                return BadRequest(new { message = "Comentario padre no encontrado" });
        }
        var review = new PartReview
        {
            PartId = partId,
            UserId = body.UserId.Value,
            ParentId = body.ParentId,
            Rating = body.ParentId == null ? body.Rating : null,
            Body = body.Body.Trim(),
            CreatedAt = DateTime.UtcNow
        };
        if (review.Rating.HasValue && (review.Rating < 1 || review.Rating > 5))
            review.Rating = null;
        _db.PartReviews.Add(review);
        await _db.SaveChangesAsync(ct);
        var user = await _db.AppUsers.FindAsync(new object[] { review.UserId }, ct);
        return StatusCode(201, new ComentarioDto
        {
            reviewId = review.ReviewId,
            partId = review.PartId,
            userId = review.UserId,
            userEmail = user?.Email,
            fullName = user?.FullName,
            parentId = review.ParentId,
            rating = review.Rating,
            body = review.Body,
            createdAt = review.CreatedAt,
            children = new List<ComentarioDto>()
        });
    }

    private static List<ComentarioDto> BuildTree(List<ComentarioDto> flat, long? parentId)
    {
        return flat
            .Where(c => c.parentId == parentId)
            .Select(c =>
            {
                c.children = BuildTree(flat, c.reviewId);
                return c;
            })
            .ToList();
    }
}

public class ComentarioDto
{
    public long reviewId { get; set; }
    public long partId { get; set; }
    public long userId { get; set; }
    public string? userEmail { get; set; }
    public string? fullName { get; set; }
    public long? parentId { get; set; }
    public int? rating { get; set; }
    public string body { get; set; } = "";
    public DateTime? createdAt { get; set; }
    public List<ComentarioDto> children { get; set; } = new();
}

public class CreateComentarioRequest
{
    public long? UserId { get; set; }
    public long? ParentId { get; set; }
    public int? Rating { get; set; }
    public string? Body { get; set; }
}
