package com.agencias.backend.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtro CORS: un solo origen por respuesta para evitar "cannot contain more than one origin".
 * Usa putSingle para no duplicar cabeceras (evita que Safari rechace la petición).
 */
@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String MAX_AGE = "Access-Control-Max-Age";

    private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String VARY = "Vary";

    private static Set<String> parseAllowedOrigins() {
        String env = System.getenv("CORS_ALLOWED_ORIGINS");
        String raw = (env != null && !env.isBlank())
            ? env
            : ConfigLoader.loadProperties().getProperty("cors.allowed.origins", "*");

        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String resolveAllowedOrigin(String requestOrigin, Set<String> allowedOrigins) {
        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            return "*";
        }
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return null;
        }
        return allowedOrigins.contains(requestOrigin) ? requestOrigin : null;
    }

    /** Un solo valor por cabecera para cumplir con CORS en todos los navegadores. */
    private static void setCorsHeaders(MultivaluedMap<String, Object> headers, String requestOrigin) {
        Set<String> allowedOrigins = parseAllowedOrigins();
        String allowOrigin = resolveAllowedOrigin(requestOrigin, allowedOrigins);
        if (allowOrigin == null) {
            return;
        }
        headers.putSingle(ALLOW_ORIGIN, allowOrigin);
        headers.putSingle(ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        headers.putSingle(ALLOW_HEADERS, "Content-Type, Authorization, X-Admin-User-Id, X-Distributor-Api-Key, X-Distributor-User-Id");
        headers.putSingle(MAX_AGE, "86400");
        headers.putSingle(ALLOW_CREDENTIALS, "false");
        headers.putSingle(VARY, "Origin");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String requestOrigin = requestContext.getHeaderString("Origin");
            MultivaluedMap<String, Object> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
            setCorsHeaders(headers, requestOrigin);
            Response.ResponseBuilder rb = Response.noContent();
            for (var entry : headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    rb.header(entry.getKey(), entry.getValue().get(0));
                }
            }
            Response response = rb.build();
            requestContext.abortWith(response);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                      ContainerResponseContext responseContext) throws IOException {
        setCorsHeaders(responseContext.getHeaders(), requestContext.getHeaderString("Origin"));
    }
}
