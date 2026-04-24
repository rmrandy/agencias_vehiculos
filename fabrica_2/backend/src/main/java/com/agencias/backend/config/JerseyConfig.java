package com.agencias.backend.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * Aplicación JAX-RS raíz ({@code /api}). Descubre recursos en
 * {@code com.agencias.backend.controller}, registra JSON (Jackson), carga de archivos
 * ({@link org.glassfish.jersey.media.multipart.MultiPartFeature}), CORS y
 * {@link com.agencias.backend.config.DistributorApiKeyFilter} para llamadas del distribuidor.
 */
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.agencias.backend.controller");
        register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(CorsFilter.class);
        register(DistributorApiKeyFilter.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
