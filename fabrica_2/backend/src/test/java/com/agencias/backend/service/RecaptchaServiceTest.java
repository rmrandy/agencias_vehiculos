package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RecaptchaServiceTest {

    @Test
    void verify_tokenVacio() {
        RecaptchaService svc = new RecaptchaService("secret");
        assertFalse(svc.verify(null));
        assertFalse(svc.verify(""));
    }

    @Test
    void verifyOrThrow() {
        RecaptchaService svc = new RecaptchaService("secret");
        assertThrows(IllegalArgumentException.class, () -> svc.verifyOrThrow(null));
    }
}
