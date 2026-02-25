package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.controller.dto.LoginRequest;
import com.agencias.backend.controller.dto.UsuarioResponse;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@jakarta.inject.Singleton
public class AuthResource {
    private final UserService userService;

    public AuthResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.userService = new UserService(emf);
    }

    /**
     * Login con email y contraseña. Devuelve el usuario (sin contraseña) si las credenciales son correctas.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        if (req == null || req.getEmail() == null || req.getPassword() == null) {
            return Response.status(400).entity(new ErrorResponse(400, "Email y contraseña son obligatorios")).build();
        }
        AppUser user = userService.login(req.getEmail(), req.getPassword());
        if (user == null) {
            return Response.status(401).entity(new ErrorResponse(401, "Credenciales incorrectas")).build();
        }
        return Response.ok(UsuarioResponse.from(user)).build();
    }
}
