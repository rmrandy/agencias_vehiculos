package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validaciones de {@link VehicleService} que no requieren base de datos (fallan antes de {@code repo.save}).
 * {@code emf} puede ser null: el repositorio no se usa en estas rutas.
 */
class VehicleServiceTest {

    @Test
    void create_rejectsBlankUniversalCode() {
        EntityManagerFactory emf = null;
        VehicleService svc = new VehicleService(emf);
        assertThrows(IllegalArgumentException.class, () -> svc.create("   ", "Ford", "Focus", 2020));
    }

    @Test
    void create_rejectsNullUniversalCode() {
        EntityManagerFactory emf = null;
        VehicleService svc = new VehicleService(emf);
        assertThrows(IllegalArgumentException.class, () -> svc.create(null, "Ford", "Focus", 2020));
    }
}
