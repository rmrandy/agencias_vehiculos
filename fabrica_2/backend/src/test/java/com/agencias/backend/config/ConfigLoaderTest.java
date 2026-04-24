package com.agencias.backend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

    @Test
    void loadProperties_noLanza_yDevuelveInstancia() {
        Properties props = ConfigLoader.loadProperties();
        assertNotNull(props);
        assertTrue(props instanceof Properties);
    }
}
