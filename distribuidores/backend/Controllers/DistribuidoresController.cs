using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Controllers;

[ApiController]
[Route("api/[controller]")]
public class DistribuidoresController : ControllerBase
{
    private readonly AppDbContext _db;

    public DistribuidoresController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<ActionResult<List<Distribuidor>>> GetAll(CancellationToken ct)
    {
        var list = await _db.Distribuidores.ToListAsync(ct);
        return Ok(list);
    }

    [HttpGet("{id:int}")]
    public async Task<ActionResult<Distribuidor>> GetById(int id, CancellationToken ct)
    {
        var item = await _db.Distribuidores.FindAsync([id], ct);
        if (item == null)
            return NotFound(new { status = 404, message = "Distribuidor no encontrado" });
        return Ok(item);
    }

    [HttpPost]
    public async Task<ActionResult<Distribuidor>> Create([FromBody] Distribuidor input, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(input.Nombre))
            return BadRequest(new { status = 400, message = "El nombre es requerido" });

        input.Id = 0;
        _db.Distribuidores.Add(input);
        await _db.SaveChangesAsync(ct);
        return CreatedAtAction(nameof(GetById), new { id = input.Id }, input);
    }

    [HttpPut("{id:int}")]
    public async Task<ActionResult<Distribuidor>> Update(int id, [FromBody] Distribuidor input, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(input.Nombre))
            return BadRequest(new { status = 400, message = "El nombre es requerido" });

        var existing = await _db.Distribuidores.FindAsync([id], ct);
        if (existing == null)
            return NotFound(new { status = 404, message = "Distribuidor no encontrado" });

        existing.Nombre = input.Nombre;
        existing.Contacto = input.Contacto;
        existing.Email = input.Email;
        existing.Telefono = input.Telefono;
        await _db.SaveChangesAsync(ct);
        return Ok(existing);
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Delete(int id, CancellationToken ct)
    {
        var existing = await _db.Distribuidores.FindAsync([id], ct);
        if (existing == null)
            return NotFound(new { status = 404, message = "Distribuidor no encontrado" });

        _db.Distribuidores.Remove(existing);
        await _db.SaveChangesAsync(ct);
        return NoContent();
    }
}
