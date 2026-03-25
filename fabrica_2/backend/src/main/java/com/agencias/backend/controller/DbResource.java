package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint para validar la conexión a la base de datos Oracle.
 * GET /api/db devuelve {"status":"ok","database":"connected"} si la conexión funciona.
 */
@Path("/db")
public class DbResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDb() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        try (EntityManager em = emf.createEntityManager()) {
            // Consulta simple para verificar conexión (Oracle)
            em.createNativeQuery("SELECT 1 FROM DUAL").getSingleResult();
            DbStatus status = new DbStatus("ok", "connected");
            return Response.ok(status).build();
        } catch (Exception e) {
            DbStatus status = new DbStatus("error", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(status).build();
        }
    }

    public static class DbStatus {
        private String status;
        private String database;

        public DbStatus() {}
        public DbStatus(String status, String database) {
            this.status = status;
            this.database = database;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
    }
}
