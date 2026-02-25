package com.agencias.backend.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.agencias.backend.controller");
        register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(CorsFilter.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
