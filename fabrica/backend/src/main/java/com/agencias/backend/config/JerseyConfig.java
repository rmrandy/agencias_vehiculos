package com.agencias.backend.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.agencias.backend.controller");
        register(JacksonFeature.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
