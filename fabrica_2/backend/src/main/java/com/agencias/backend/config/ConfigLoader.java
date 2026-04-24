package com.agencias.backend.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    public static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar application.properties: " + e.getMessage());
        }
        return props;
    }
}
