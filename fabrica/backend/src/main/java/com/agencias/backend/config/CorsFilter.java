package com.agencias.backend.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

/**
 * Filtro CORS: un solo origen por respuesta para evitar "cannot contain more than one origin".
 * Usa putSingle para no duplicar cabeceras (evita que Safari rechace la petición).
 */
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String MAX_AGE = "Access-Control-Max-Age";

    /** Un solo valor por cabecera para cumplir con CORS en todos los navegadores. */
    private static void setCorsHeaders(MultivaluedMap<String, Object> headers) {
        headers.putSingle(ALLOW_ORIGIN, "*");
        headers.putSingle(ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        headers.putSingle(ALLOW_HEADERS, "Content-Type, Authorization, X-Admin-User-Id");
        headers.putSingle(MAX_AGE, "86400");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            Response response = Response.noContent()
                .header(ALLOW_ORIGIN, "*")
                .header(ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS")
                .header(ALLOW_HEADERS, "Content-Type, Authorization, X-Admin-User-Id")
                .header(MAX_AGE, "86400")
                .build();
            requestContext.abortWith(response);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                      ContainerResponseContext responseContext) throws IOException {
        setCorsHeaders(responseContext.getHeaders());
    }
}
