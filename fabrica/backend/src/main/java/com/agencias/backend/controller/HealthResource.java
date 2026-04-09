package com.agencias.backend.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Comprobación de vida del servicio ({@code GET /api/health}). */
@Path("/health")
public class HealthResource {
    /**
     * @return 200 JSON {@code {"status":"ok"}} si el proceso responde
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(new HealthStatus("ok")).build();
    }

    public static class HealthStatus {
        private String status;

        public HealthStatus() {
        }

        public HealthStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
