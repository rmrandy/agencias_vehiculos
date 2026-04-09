package com.agencias.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos de totales de pedido sin persistencia. */
public final class OrderPricing {

    private OrderPricing() {
    }

    /**
     * Aplica descuento porcentual si es positivo; si no hay descuento válido devuelve el subtotal.
     */
    public static BigDecimal applyEnterpriseDiscount(BigDecimal subtotal, BigDecimal discountPercent) {
        if (subtotal == null) {
            throw new IllegalArgumentException("subtotal requerido");
        }
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return subtotal;
        }
        BigDecimal discount = subtotal.multiply(discountPercent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return subtotal.subtract(discount);
    }
}
