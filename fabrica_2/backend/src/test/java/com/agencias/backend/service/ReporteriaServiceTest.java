package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validaciones tempranas de {@link ReporteriaService#registerEvent} sin consultar repositorios.
 */
class ReporteriaServiceTest {

    @Test
    void registerEvent_rejectsBlankEventType() {
        EntityManagerFactory emf = null;
        ReporteriaService svc = new ReporteriaService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.registerEvent("   ", 1L, null, null, null, null));
    }

    @Test
    void registerEvent_rejectsMissingPartIdAndNumber() {
        EntityManagerFactory emf = null;
        ReporteriaService svc = new ReporteriaService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.registerEvent("VIEW", null, null, null, null, null));
    }
}
