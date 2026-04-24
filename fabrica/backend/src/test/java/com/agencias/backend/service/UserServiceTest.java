package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Rutas de {@link UserService#login} y {@link UserService#resolveOrCreatePortalUser} sin acceso a repositorio.
 */
class UserServiceTest {

    @Test
    void login_returnsNullWhenEmailNull() {
        EntityManagerFactory emf = null;
        UserService svc = new UserService(emf);
        assertNull(svc.login(null, "secret"));
    }

    @Test
    void login_returnsNullWhenEmailBlank() {
        EntityManagerFactory emf = null;
        UserService svc = new UserService(emf);
        assertNull(svc.login("  ", "secret"));
    }

    @Test
    void login_returnsNullWhenPasswordNull() {
        EntityManagerFactory emf = null;
        UserService svc = new UserService(emf);
        assertNull(svc.login("a@b.com", null));
    }

    @Test
    void resolveOrCreatePortalUser_rejectsBlankEmail() {
        EntityManagerFactory emf = null;
        UserService svc = new UserService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.resolveOrCreatePortalUser("  ", "Nombre"));
    }
}
