package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.Brand;
import com.agencias.backend.service.BrandService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Path("/marcas")
@jakarta.inject.Singleton
public class BrandResource {
    private final BrandService service;

    public BrandResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new BrandService(emf);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            
            // Procesar imagen si existe
            byte[] imageData = null;
            String imageType = null;
            if (body.containsKey("imageData") && body.get("imageData") != null) {
                String base64Data = (String) body.get("imageData");
                imageType = (String) body.get("imageType");
                
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                imageData = Base64.getDecoder().decode(base64Data);
            }
            
            Brand b = service.create(name);
            
            // Agregar imagen si existe
            if (imageData != null) {
                b = service.updateImage(b.getBrandId(), imageData, imageType);
            }
            
            return Response.status(Response.Status.CREATED).entity(b).build();
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
            List<Brand> list = service.listAll();
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        Brand b = service.getById(id);
        if (b == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Marca no encontrada")).build();
        }
        return Response.ok(b).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            Brand b = service.update(id, name);
            return Response.ok(b).build();
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
