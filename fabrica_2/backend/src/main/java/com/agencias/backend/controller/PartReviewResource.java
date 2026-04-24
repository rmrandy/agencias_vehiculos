package com.agencias.backend.controller;

import com.agencias.backend.controller.dto.ComentariosResponse;
import com.agencias.backend.service.PartReviewService;
import com.agencias.backend.service.PartService;
import com.agencias.backend.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.agencias.backend.config.ConfigLoader;
import com.agencias.backend.config.DatabaseConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Comentarios y valoraciones (1-5 estrellas) sobre repuestos.
 * Solo usuarios registrados pueden crear comentarios.
 * Comentarios multinivel (respuestas a comentarios).
 */
@Path("/repuestos/{partId}/comentarios")
@jakarta.inject.Singleton
public class PartReviewResource {

    private static final String DIST_API_KEY_HEADER = "X-Distributor-Api-Key";

    private final PartReviewService reviewService;
    private final PartService partService;
    private final UserService userService;

    @Context
    private HttpHeaders headers;

    public PartReviewResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.reviewService = new PartReviewService(emf);
        this.partService = new PartService(emf);
        this.userService = new UserService(emf);
    }

    /**
     * GET /api/repuestos/{partId}/comentarios
     * Devuelve promedio de estrellas y árbol de comentarios.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("partId") Long partId) {
        if (partService.getById(partId) == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Repuesto no encontrado")).build();
        }
        ComentariosResponse resp = new ComentariosResponse();
        resp.setPromedio(reviewService.getAverageRating(partId));
        resp.setComentarios(reviewService.getTreeByPartId(partId));
        return Response.ok(resp).build();
    }

    /**
     * POST /api/repuestos/{partId}/comentarios
     * Crear comentario o respuesta. Solo usuarios registrados.
     * Body: { userId (obligatorio), body (obligatorio), rating? (1-5 solo en comentario raíz), parentId? (para respuestas) }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("partId") Long partId, Map<String, Object> body) {
        if (partService.getById(partId) == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Repuesto no encontrado")).build();
        }
        try {
            Long userId = resolveUserIdForCreate(body);
            Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
            Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
            String commentBody = (String) body.get("body");

            var review = reviewService.create(partId, userId, parentId, rating, commentBody);
            return Response.status(Response.Status.CREATED).entity(review).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Si hay allowlist de API keys y la petición trae clave válida + {@code userEmail}, se usa (o crea) un usuario en APP_USER.
     * Si no, se exige {@code userId} como hasta ahora.
     */
    private Long resolveUserIdForCreate(Map<String, Object> body) {
        Properties props = ConfigLoader.loadProperties();
        String allow = props.getProperty("distribuidoras.api.key.allowlist", "").trim();
        if (!allow.isEmpty() && headers != null) {
            Set<String> keys = new HashSet<>();
            for (String s : allow.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    keys.add(t);
                }
            }
            String headerKey = headers.getHeaderString(DIST_API_KEY_HEADER);
            if (headerKey != null && keys.contains(headerKey.trim())) {
                Object rawEmail = body.get("userEmail");
                if (rawEmail instanceof String ue && !ue.isBlank()) {
                    String fullName = body.get("userFullName") instanceof String s ? s : null;
                    return userService.resolveOrCreatePortalUser(ue, fullName).getUserId();
                }
            }
        }
        return body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
    }
}
