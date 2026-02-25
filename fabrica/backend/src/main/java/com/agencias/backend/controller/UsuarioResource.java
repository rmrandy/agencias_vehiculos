package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.controller.dto.AssignRolesRequest;
import com.agencias.backend.controller.dto.UsuarioCreateRequest;
import com.agencias.backend.controller.dto.UsuarioResponse;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.EnterpriseProfile;
import com.agencias.backend.service.EnterpriseProfileService;
import com.agencias.backend.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/usuarios")
@jakarta.inject.Singleton
public class UsuarioResource {
    private final UserService userService;
    private final EnterpriseProfileService enterpriseProfileService;

    public UsuarioResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.userService = new UserService(emf);
        this.enterpriseProfileService = new EnterpriseProfileService(emf);
    }

    /**
     * Registro de usuario. El primer usuario se crea como ADMIN; el resto como REGISTERED.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(UsuarioCreateRequest req) {
        try {
            // Crear usuario (sin validación de reCAPTCHA)
            AppUser user = userService.createUser(
                req.getEmail(),
                req.getPassword(),
                req.getFullName(),
                req.getPhone()
            );
            return Response.status(Response.Status.CREATED).entity(UsuarioResponse.from(user)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (IllegalStateException e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Error al crear usuario";
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            return Response.status(500).entity(new ErrorResponse(500, msg)).build();
        }
    }

    /**
     * Lista todos los usuarios (para panel admin). Incluye datos de perfil empresarial si aplica.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            List<AppUser> users = userService.listUsers();
            java.util.Map<Long, EnterpriseProfile> profileMap = new java.util.HashMap<>();
            for (EnterpriseProfile ep : enterpriseProfileService.listAll()) {
                profileMap.put(ep.getUserId(), ep);
            }
            List<UsuarioResponse> list = users.stream().map(u -> {
                UsuarioResponse r = UsuarioResponse.from(u);
                EnterpriseProfile ep = profileMap.get(u.getUserId());
                if (ep != null) {
                    r.setIsEnterprise(true);
                    r.setEnterpriseDiscountPercent(ep.getDiscountPercent());
                } else {
                    r.setIsEnterprise(u.getRoles() != null && u.getRoles().stream().anyMatch(role -> "ENTERPRISE".equals(role.getName())));
                }
                return r;
            }).collect(Collectors.toList());
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Obtiene un usuario por ID.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        AppUser user = userService.getById(id);
        if (user == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Usuario no encontrado")).build();
        }
        return Response.ok(UsuarioResponse.from(user)).build();
    }

    /**
     * Asigna roles a un usuario. Solo un admin puede hacerlo.
     * Header obligatorio: X-Admin-User-Id = ID del usuario administrador que realiza la acción.
     */
    @PUT
    @Path("/{id}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignRoles(
        @PathParam("id") Long userId,
        AssignRolesRequest body,
        @HeaderParam("X-Admin-User-Id") Long adminUserId
    ) {
        if (adminUserId == null) {
            return Response.status(403).entity(new ErrorResponse(403, "Se requiere el header X-Admin-User-Id")).build();
        }
        if (body == null || body.getRoleIds() == null) {
            return Response.status(400).entity(new ErrorResponse(400, "roleIds es obligatorio")).build();
        }
        try {
            AppUser user = userService.assignRoles(userId, body.getRoleIds(), adminUserId);
            return Response.ok(UsuarioResponse.from(user)).build();
        } catch (SecurityException e) {
            return Response.status(403).entity(new ErrorResponse(403, e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Obtener perfil empresarial de un usuario (admin o el propio usuario).
     * GET /api/usuarios/{id}/empresarial
     */
    @GET
    @Path("/{id}/empresarial")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmpresarial(@PathParam("id") Long userId) {
        Optional<EnterpriseProfile> opt = enterpriseProfileService.getByUserId(userId);
        if (opt.isEmpty()) {
            return Response.status(404).entity(new ErrorResponse(404, "El usuario no tiene perfil empresarial")).build();
        }
        return Response.ok(opt.get()).build();
    }

    /**
     * Admin: asignar usuario como empresarial y configurar descuento.
     * PUT /api/usuarios/{id}/empresarial
     * Body: { adminUserId, discountPercent }
     */
    @PUT
    @Path("/{id}/empresarial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putEmpresarial(@PathParam("id") Long userId, Map<String, Object> body) {
        try {
            Long adminUserId = body.get("adminUserId") != null ? ((Number) body.get("adminUserId")).longValue() : null;
            BigDecimal discountPercent = null;
            if (body.get("discountPercent") != null) {
                discountPercent = new BigDecimal(body.get("discountPercent").toString());
            }
            // Si viene adminUserId + discountPercent -> asignación por admin
            if (adminUserId != null) {
                EnterpriseProfile p = enterpriseProfileService.assignEnterprise(userId, discountPercent, adminUserId);
                return Response.ok(p).build();
            }
            // Si no, es el propio usuario empresarial actualizando su perfil (dirección, tarjeta, horario)
            String defaultAddressText = (String) body.get("defaultAddressText");
            String defaultCardToken = (String) body.get("defaultCardToken");
            String defaultCardLast4 = (String) body.get("defaultCardLast4");
            String deliveryWindow = (String) body.get("deliveryWindow");
            Long selfUserId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            if (selfUserId == null || !selfUserId.equals(userId)) {
                return Response.status(403).entity(new ErrorResponse(403, "Solo puedes actualizar tu propio perfil")).build();
            }
            EnterpriseProfile p = enterpriseProfileService.updateProfile(userId, defaultAddressText, defaultCardToken, defaultCardLast4, deliveryWindow);
            return Response.ok(p).build();
        } catch (SecurityException e) {
            return Response.status(403).entity(new ErrorResponse(403, e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Admin: quitar perfil empresarial (quita rol ENTERPRISE).
     * DELETE /api/usuarios/{id}/empresarial
     * Body: { adminUserId }
     */
    @DELETE
    @Path("/{id}/empresarial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEmpresarial(@PathParam("id") Long userId, Map<String, Object> body) {
        Long adminUserId = body != null && body.get("adminUserId") != null ? ((Number) body.get("adminUserId")).longValue() : null;
        if (adminUserId == null) {
            return Response.status(403).entity(new ErrorResponse(403, "Se requiere adminUserId en el body")).build();
        }
        try {
            enterpriseProfileService.unassignEnterprise(userId, adminUserId);
            return Response.ok(Map.of("message", "Perfil empresarial quitado")).build();
        } catch (SecurityException e) {
            return Response.status(403).entity(new ErrorResponse(403, e.getMessage())).build();
        }
    }
}
