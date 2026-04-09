package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserRegistrationValidatorTest {

    @Test
    void emailObligatorio() {
        assertThrows(IllegalArgumentException.class, () ->
            UserRegistrationValidator.validateEmailAndPassword(null, "123456"));
        assertThrows(IllegalArgumentException.class, () ->
            UserRegistrationValidator.validateEmailAndPassword("  ", "123456"));
    }

    @Test
    void passwordMinimo() {
        assertThrows(IllegalArgumentException.class, () ->
            UserRegistrationValidator.validateEmailAndPassword("a@b.com", null));
        assertThrows(IllegalArgumentException.class, () ->
            UserRegistrationValidator.validateEmailAndPassword("a@b.com", "12345"));
    }

    @Test
    void valido() {
        assertDoesNotThrow(() ->
            UserRegistrationValidator.validateEmailAndPassword("a@b.com", "123456"));
    }
}
