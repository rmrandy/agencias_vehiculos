package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validaciones de {@link PartService#create} sin persistencia (argumentos inválidos).
 */
class PartServiceTest {

    @Test
    void create_rejectsBlankPartNumber() {
        EntityManagerFactory emf = null;
        PartService svc = new PartService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.create(1L, 1L, "  ", "Título", null, null, BigDecimal.TEN, 0, 5, null));
    }

    @Test
    void create_rejectsBlankTitle() {
        EntityManagerFactory emf = null;
        PartService svc = new PartService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.create(1L, 1L, "PN-1", "  ", null, null, BigDecimal.TEN, 0, 5, null));
    }

    @Test
    void create_rejectsNegativePrice() {
        EntityManagerFactory emf = null;
        PartService svc = new PartService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.create(1L, 1L, "PN-2", "Ok", null, null, BigDecimal.valueOf(-1), 0, 5, null));
    }
}
