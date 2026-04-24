package com.agencias.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void constructorYGetters() {
        ErrorResponse e = new ErrorResponse(400, "Bad");
        assertEquals(400, e.getStatus());
        assertEquals("Bad", e.getMessage());
        e.setStatus(500);
        e.setMessage("Err");
        assertEquals(500, e.getStatus());
        assertEquals("Err", e.getMessage());
    }
}
