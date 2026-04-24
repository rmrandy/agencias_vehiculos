package com.agencias.backend.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DatabaseConfig {
    private static EntityManagerFactory emf;

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Properties props = ConfigLoader.loadProperties();
            
            String dbHost = System.getenv("DB_HOST");
            if (dbHost == null) dbHost = props.getProperty("DB_HOST", "localhost");
            
            String dbPort = System.getenv("DB_PORT");
            if (dbPort == null) dbPort = props.getProperty("DB_PORT", "1521");
            
            String dbService = System.getenv("DB_SERVICE");
            if (dbService == null) dbService = props.getProperty("DB_SERVICE", "XEPDB1");
            
            String dbUser = System.getenv("DB_USER");
            if (dbUser == null) dbUser = props.getProperty("DB_USER", "SYS");
            
            String dbPass = System.getenv("DB_PASS");
            if (dbPass == null) dbPass = props.getProperty("DB_PASS", "123");

            // Usar formato de Service Name con / en lugar de SID con :
            // Si el usuario es SYS, agregar el parámetro internal_logon=SYSDBA
            String jdbcUrl = String.format("jdbc:oracle:thin:@%s:%s/%s", dbHost, dbPort, dbService);
            if ("SYS".equalsIgnoreCase(dbUser)) {
                jdbcUrl += "?internal_logon=SYSDBA";
            }

            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
            properties.put("jakarta.persistence.jdbc.user", dbUser);
            properties.put("jakarta.persistence.jdbc.password", dbPass);
            properties.put("jakarta.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
            properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
            properties.put("hibernate.hbm2ddl.auto", props.getProperty("hibernate.hbm2ddl.auto", "update"));
            properties.put("hibernate.show_sql", props.getProperty("hibernate.show_sql", "true"));
            properties.put("hibernate.format_sql", props.getProperty("hibernate.format_sql", "true"));
            // Deshabilitar escaneo automático para evitar problemas con Jandex en JAR shaded
            // Ya tenemos las clases especificadas explícitamente en persistence.xml
            properties.put("jakarta.persistence.scan.discovery", "false");
            properties.put("hibernate.archive.autodetection", "none");

            emf = Persistence.createEntityManagerFactory("default", properties);
        }
        return emf;
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
