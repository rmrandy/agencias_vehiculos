package com.agencias.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderFlowRulesTest {

    @Test
    void statusIndex_flujoPrincipal() {
        assertEquals(0, OrderFlowRules.statusIndex("INITIATED"));
        assertEquals(1, OrderFlowRules.statusIndex("PREPARING"));
        assertEquals(2, OrderFlowRules.statusIndex("SHIPPED"));
        assertEquals(3, OrderFlowRules.statusIndex("DELIVERED"));
    }

    @Test
    void statusIndex_legacyYCancelado() {
        assertEquals(1, OrderFlowRules.statusIndex("CONFIRMED"));
        assertEquals(1, OrderFlowRules.statusIndex("IN_PREPARATION"));
        assertEquals(OrderFlowRules.STATUS_FLOW.size(), OrderFlowRules.statusIndex("CANCELLED"));
    }

    @Test
    void statusIndex_nulosEInvalidos() {
        assertEquals(-1, OrderFlowRules.statusIndex(null));
        assertEquals(-1, OrderFlowRules.statusIndex("UNKNOWN"));
    }

    @Test
    void normalizeOrderOrigin() {
        assertEquals("FABRICA_WEB", OrderFlowRules.normalizeOrderOrigin(null));
        assertEquals("FABRICA_WEB", OrderFlowRules.normalizeOrderOrigin("   "));
        assertEquals("DISTRIBUIDORA", OrderFlowRules.normalizeOrderOrigin("x-distributor"));
        assertEquals("DISTRIBUIDORA", OrderFlowRules.normalizeOrderOrigin("pedido-distribuidor"));
        assertEquals("FABRICA_WEB", OrderFlowRules.normalizeOrderOrigin("web"));
    }
}
