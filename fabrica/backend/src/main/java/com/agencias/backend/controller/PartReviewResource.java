package com.agencias.backend.controller;

import com.agencias.backend.controller.dto.ComentariosResponse;
import com.agencias.backend.service.PartReviewService;
import com.agencias.backend.service.PartService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.agencias.backend.config.DatabaseConfig;

import java.util.List;
import java.util.Map;

/**
 * Comentarios y valoraciones (1-5 estrellas) sobre repuestos.
 * Solo usuarios registrados pueden crear comentarios.
 * Comentarios multinivel (respuestas a comentarios).
 */
@Path("/repuestos/{partId}/comentarios")
@jakarta.inject.Singleton
public class PartReviewResource {

    private final PartReviewService reviewService;
    private final PartService partService;

    public PartReviewResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.reviewService = new PartReviewService(emf);
        this.partService = new PartService(emf);
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
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
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
}
