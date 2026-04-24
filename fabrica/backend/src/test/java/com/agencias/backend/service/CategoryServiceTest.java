package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link CategoryService#create} valida nombre con {@link CatalogNameValidator}. */
class CategoryServiceTest {

    @Test
    void create_rejectsBlankName() {
        EntityManagerFactory emf = null;
        CategoryService svc = new CategoryService(emf);
        assertThrows(IllegalArgumentException.class, () -> svc.create("\t\n", null));
    }
}
