package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.Repuesto;
import com.agencias.backend.service.RepuestoService;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/repuestos")
@Singleton
public class RepuestoResource {
    private final RepuestoService service;

    public RepuestoResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new RepuestoService(emf);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearRepuesto(Repuesto repuesto) {
        try {
            Repuesto creado = service.crearRepuesto(repuesto);
            return Response.status(Response.Status.CREATED).entity(creado).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Error interno del servidor")).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerTodos() {
        try {
            List<Repuesto> repuestos = service.obtenerTodos();
            return Response.ok(repuestos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Error interno del servidor")).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerPorId(@PathParam("id") Long id) {
        try {
            return service.obtenerPorId(id)
                    .map(repuesto -> Response.ok(repuesto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse(404, "Repuesto no encontrado")).build());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Error interno del servidor")).build();
        }
    }
}
