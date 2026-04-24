package com.agencias.backend.service;

/** Nombre obligatorio para marcas, categorías y catálogos similares. */
public final class CatalogNameValidator {

    private CatalogNameValidator() {
    }

    public static String requireNonBlankName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }
}
