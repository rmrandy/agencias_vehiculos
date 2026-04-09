package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PartReviewInputValidatorTest {

    @Test
    void userIdRequerido() {
        assertThrows(IllegalArgumentException.class, () ->
            PartReviewInputValidator.validate(null, null, null, "hola"));
    }

    @Test
    void bodyNoVacio() {
        assertThrows(IllegalArgumentException.class, () ->
            PartReviewInputValidator.validate(1L, null, null, null));
        assertThrows(IllegalArgumentException.class, () ->
            PartReviewInputValidator.validate(1L, null, null, "   "));
    }

    @Test
    void ratingRaizFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () ->
            PartReviewInputValidator.validate(1L, null, 0, "ok"));
        assertThrows(IllegalArgumentException.class, () ->
            PartReviewInputValidator.validate(1L, null, 6, "ok"));
    }

    @Test
    void normalizedRating_respuestaIgnoraRating() {
        assertNull(PartReviewInputValidator.normalizedRating(99L, 5));
        assertEquals(4, PartReviewInputValidator.normalizedRating(null, 4));
        assertNull(PartReviewInputValidator.normalizedRating(null, null));
    }
}
