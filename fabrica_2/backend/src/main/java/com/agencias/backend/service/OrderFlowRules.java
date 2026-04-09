package com.agencias.backend.service;

import java.util.List;
import java.util.Locale;

/**
 * Reglas puras del flujo de estados de pedido y normalización de origen (sin acceso a BD).
 */
public final class OrderFlowRules {

    public static final List<String> STATUS_FLOW = List.of(
        "INITIATED",
        "PREPARING",
        "SHIPPED",
        "DELIVERED"
    );
    public static final String STATUS_CANCELLED = "CANCELLED";

    private OrderFlowRules() {
    }

    /**
     * Índice en el flujo lineal; CANCELLED tiene índice especial ({@code STATUS_FLOW.size()}).
     * Estados legacy CONFIRMED / IN_PREPARATION se mapean a PREPARING (índice 1).
     */
    public static int statusIndex(String status) {
        if (status == null) {
            return -1;
        }
        String u = status.toUpperCase(Locale.ROOT);
        if (STATUS_CANCELLED.equals(u)) {
            return STATUS_FLOW.size();
        }
        for (int i = 0; i < STATUS_FLOW.size(); i++) {
            if (STATUS_FLOW.get(i).equals(u)) {
                return i;
            }
        }
        if ("CONFIRMED".equals(u) || "IN_PREPARATION".equals(u)) {
            return 1;
        }
        return -1;
    }

    public static String normalizeOrderOrigin(String header) {
        if (header == null || header.isBlank()) {
            return "FABRICA_WEB";
        }
        String u = header.trim().toLowerCase(Locale.ROOT);
        if (u.contains("distributor") || u.contains("distribuidor")) {
            return "DISTRIBUIDORA";
        }
        return "FABRICA_WEB";
    }
}
