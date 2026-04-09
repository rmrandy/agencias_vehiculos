using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using BackendDistribuidores.Data;
using BackendDistribuidores.Models;

namespace BackendDistribuidores.Controllers;

/// <summary>
/// CRUD sobre la entidad legacy <see cref="Distribuidor"/> (tabla propia, distinta del resto del dominio e-commerce).
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class DistribuidoresController : ControllerBase
{
    private readonly AppDbContext _db;

    public DistribuidoresController(AppDbContext db)
    {
        _db = db;
    }

    /// <summary>Lista todos los registros de distribuidores legacy.</summary>
    [HttpGet]
    public async Task<ActionResult<List<Distribuidor>>> GetAll(CancellationToken ct)
    {
        var list = await _db.Distribuidores.ToListAsync(ct);
        return Ok(list);
    }

    /// <summary>Obtiene un distribuidor por id entero.</summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<Distribuidor>> GetById(int id, CancellationToken ct)
    {
        var item = await _db.Distribuidores.FindAsync([id], ct);
        if (item == null)
            return NotFound(new { status = 404, message = "Distribuidor no encontrado" });
        return Ok(item);
    }

    /// <summary>Crea un distribuidor; el nombre es obligatorio.</summary>
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

    /// <summary>Actualiza campos del distribuidor existente.</summary>
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

    /// <summary>Elimina el distribuidor indicado.</summary>
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
