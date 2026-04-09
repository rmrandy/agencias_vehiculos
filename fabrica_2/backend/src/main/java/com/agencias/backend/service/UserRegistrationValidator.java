package com.agencias.backend.service;

/** Validación de entrada para registro de usuario (sin persistencia). */
public final class UserRegistrationValidator {

    private UserRegistrationValidator() {
    }

    public static void validateEmailAndPassword(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
    }
}
