package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CatalogNameValidatorTest {

    @Test
    void requiereNombre() {
        assertThrows(IllegalArgumentException.class, () -> CatalogNameValidator.requireNonBlankName(null));
        assertThrows(IllegalArgumentException.class, () -> CatalogNameValidator.requireNonBlankName("  "));
    }

    @Test
    void trim() {
        assertEquals("Marca", CatalogNameValidator.requireNonBlankName("  Marca  "));
    }
}
