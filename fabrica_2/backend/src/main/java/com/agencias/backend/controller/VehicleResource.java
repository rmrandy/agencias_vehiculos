package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.Vehicle;
import com.agencias.backend.service.VehicleService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/vehiculos")
@jakarta.inject.Singleton
public class VehicleResource {
    private final VehicleService service;

    public VehicleResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new VehicleService(emf);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Map<String, Object> body) {
        try {
            String code = (String) body.get("universalVehicleCode");
            String make = (String) body.get("make");
            String line = (String) body.get("line");
            Integer year = body.get("yearNumber") != null ? ((Number) body.get("yearNumber")).intValue() : null;
            Vehicle v = service.create(code, make, line, year);
            return Response.status(Response.Status.CREATED).entity(v).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            List<Vehicle> list = service.listAll();
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        Vehicle v = service.getById(id);
        if (v == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Vehículo no encontrado")).build();
        }
        return Response.ok(v).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            String code = (String) body.get("universalVehicleCode");
            String make = (String) body.get("make");
            String line = (String) body.get("line");
            Integer year = body.get("yearNumber") != null ? ((Number) body.get("yearNumber")).intValue() : null;
            Vehicle v = service.update(id, code, make, line, year);
            return Response.ok(v).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            service.delete(id);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }
}
