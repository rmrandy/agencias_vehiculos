package com.agencias.backend;

import com.agencias.backend.config.ConfigLoader;
import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.config.JerseyConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        try {
            // Cargar configuración
            Properties props = ConfigLoader.loadProperties();
            
            // Obtener puerto de variable de entorno o properties
            String portEnv = System.getenv("PORT");
            int port = portEnv != null ? Integer.parseInt(portEnv) 
                      : Integer.parseInt(props.getProperty("PORT", "8080"));
            
            // Inicializar EntityManagerFactory
            DatabaseConfig.getEntityManagerFactory();
            System.out.println("EntityManagerFactory inicializado correctamente");
            
            // Configurar Jersey
            JerseyConfig jerseyConfig = new JerseyConfig();
            
            // Configurar Jetty
            Server server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            
            ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyConfig));
            jerseyServlet.setInitOrder(0);
            context.addServlet(jerseyServlet, "/api/*");
            
            server.setHandler(context);
            
            // Agregar shutdown hook para cerrar EntityManagerFactory
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    DatabaseConfig.close();
                } catch (Exception ignored) { }
            }));
            
            // Iniciar servidor
            server.start();
            System.out.println("Servidor iniciado en http://localhost:" + port);
            System.out.println("API disponible en http://localhost:" + port + "/api");
            System.out.println("Health check: http://localhost:" + port + "/api/health");
            
            server.join();
        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
