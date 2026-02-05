package com.agencias.backend.service;

import com.agencias.backend.model.Repuesto;
import com.agencias.backend.repository.RepuestoRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

public class RepuestoService {
    private final RepuestoRepository repository;

    public RepuestoService(EntityManagerFactory emf) {
        this.repository = new RepuestoRepository(emf);
    }

    public Repuesto crearRepuesto(Repuesto repuesto) {
        if (repuesto.getNombre() == null || repuesto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del repuesto es requerido");
        }
        if (repuesto.getPrecio() == null || repuesto.getPrecio().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero");
        }
        return repository.save(repuesto);
    }

    public List<Repuesto> obtenerTodos() {
        return repository.findAll();
    }

    public Optional<Repuesto> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    public void eliminar(Long id) {
        if (!repository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Repuesto no encontrado");
        }
        repository.delete(id);
    }
}
