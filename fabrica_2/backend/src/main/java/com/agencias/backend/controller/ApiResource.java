package com.agencias.backend.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Responde en GET /api para que la raíz de la API devuelva información útil
 * (evita 404 al abrir http://localhost:8080/api en el navegador).
 */
@Path("")
public class ApiResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response api() {
        ApiInfo info = new ApiInfo();
        info.setApi("ok");
        info.setHealth("/api/health");
        info.setRepuestos("/api/repuestos");
        return Response.ok(info).build();
    }

    public static class ApiInfo {
        private String api;
        private String health;
        private String repuestos;

        public String getApi() { return api; }
        public void setApi(String api) { this.api = api; }
        public String getHealth() { return health; }
        public void setHealth(String health) { this.health = health; }
        public String getRepuestos() { return repuestos; }
        public void setRepuestos(String repuestos) { this.repuestos = repuestos; }
    }
}
