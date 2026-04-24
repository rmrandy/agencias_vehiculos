package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link BrandService#create} delega el nombre a {@link CatalogNameValidator} antes de persistir. */
class BrandServiceTest {

    @Test
    void create_rejectsBlankName() {
        EntityManagerFactory emf = null;
        BrandService svc = new BrandService(emf);
        assertThrows(IllegalArgumentException.class, () -> svc.create("   "));
    }
}
