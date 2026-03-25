package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.Role;
import com.agencias.backend.service.RoleService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/roles")
@jakarta.inject.Singleton
public class RoleResource {
    private final RoleService roleService;

    public RoleResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.roleService = new RoleService(emf);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRoles() {
        try {
            List<Role> list = roleService.listRoles();
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        Role r = roleService.getById(id);
        if (r == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Rol no encontrado")).build();
        }
        return Response.ok(r).build();
    }
}
