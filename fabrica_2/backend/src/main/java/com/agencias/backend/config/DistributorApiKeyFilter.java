package com.agencias.backend.config;

import com.agencias.backend.controller.ErrorResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Si {@code distribuidoras.api.key.allowlist} tiene valores (coma-separados), exige cabecera
 * {@code X-Distributor-Api-Key} en rutas de catálogo y pedidos. Vacío = sin validación (compatibilidad).
 */
@Provider
public class DistributorApiKeyFilter implements ContainerRequestFilter {

    private static final String HEADER = "X-Distributor-Api-Key";

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ctx.getMethod())) {
            return;
        }
        String path = ctx.getUriInfo().getPath();
        if (path == null || path.isEmpty()) {
            return;
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (!p.startsWith("repuestos") && !p.startsWith("pedidos")) {
            return;
        }

        Properties props = ConfigLoader.loadProperties();
        String allow = props.getProperty("distribuidoras.api.key.allowlist", "").trim();
        if (allow.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>();
        for (String s : allow.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                keys.add(t);
            }
        }
        if (keys.isEmpty()) {
            return;
        }

        String header = ctx.getHeaderString(HEADER);
        if (header == null || !keys.contains(header.trim())) {
            ctx.abortWith(Response.status(401)
                .entity(new ErrorResponse(401, "X-Distributor-Api-Key no autorizada"))
                .build());
        }
    }
}
