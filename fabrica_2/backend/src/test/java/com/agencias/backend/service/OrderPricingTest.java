package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderPricingTest {

    @Test
    void sinDescuento() {
        BigDecimal sub = new BigDecimal("100.00");
        assertEquals(sub, OrderPricing.applyEnterpriseDiscount(sub, null));
        assertEquals(sub, OrderPricing.applyEnterpriseDiscount(sub, BigDecimal.ZERO));
    }

    @Test
    void conDescuento() {
        BigDecimal sub = new BigDecimal("100.00");
        BigDecimal total = OrderPricing.applyEnterpriseDiscount(sub, new BigDecimal("10"));
        assertEquals(new BigDecimal("90.00"), total);
    }

    @Test
    void subtotalNull_lanza() {
        assertThrows(IllegalArgumentException.class, () ->
            OrderPricing.applyEnterpriseDiscount(null, new BigDecimal("5")));
    }
}
