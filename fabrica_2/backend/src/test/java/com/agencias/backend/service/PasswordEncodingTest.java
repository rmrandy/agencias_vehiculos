package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordEncodingTest {

    @Test
    void hashYVerify_redondo() {
        String hash = PasswordEncoding.hash("secreto123");
        assertTrue(PasswordEncoding.verify("secreto123", hash));
        assertFalse(PasswordEncoding.verify("otra", hash));
    }

    @Test
    void verify_conNull() {
        assertFalse(PasswordEncoding.verify(null, "x"));
        assertFalse(PasswordEncoding.verify("x", null));
    }
}
