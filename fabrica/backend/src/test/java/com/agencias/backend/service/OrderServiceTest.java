package com.agencias.backend.service;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reglas de entrada de {@link OrderService#createOrder} sin ejecutar flujo completo contra Oracle.
 */
class OrderServiceTest {

    @Test
    void createOrder_rejectsNullItems() {
        EntityManagerFactory emf = null;
        OrderService svc = new OrderService(emf);
        assertThrows(IllegalArgumentException.class, () -> svc.createOrder(1L, null, null));
    }

    @Test
    void createOrder_rejectsEmptyItems() {
        EntityManagerFactory emf = null;
        OrderService svc = new OrderService(emf);
        assertThrows(IllegalArgumentException.class,
            () -> svc.createOrder(1L, Collections.emptyList(), null));
    }
}
