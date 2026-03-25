package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Path("/images")
@Singleton
public class ImageResource {
    private final EntityManagerFactory emf;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public ImageResource() {
        this.emf = DatabaseConfig.getEntityManagerFactory();
    }

    /**
     * Endpoint para servir imágenes desde la base de datos
     * GET /api/images/{entityType}/{id}
     * entityType: part, category, brand, vehicle
     */
    @GET
    @Path("/{entityType}/{id}")
    public Response getImage(@PathParam("entityType") String entityType, @PathParam("id") Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Object entity = null;
            byte[] imageData = null;
            String imageType = null;

            switch (entityType.toLowerCase()) {
                case "part":
                    entity = em.find(com.agencias.backend.model.Part.class, id);
                    if (entity != null) {
                        imageData = ((com.agencias.backend.model.Part) entity).getImageData();
                        imageType = ((com.agencias.backend.model.Part) entity).getImageType();
                    }
                    break;
                case "category":
                    entity = em.find(com.agencias.backend.model.Category.class, id);
                    if (entity != null) {
                        imageData = ((com.agencias.backend.model.Category) entity).getImageData();
                        imageType = ((com.agencias.backend.model.Category) entity).getImageType();
                    }
                    break;
                case "brand":
                    entity = em.find(com.agencias.backend.model.Brand.class, id);
                    if (entity != null) {
                        imageData = ((com.agencias.backend.model.Brand) entity).getImageData();
                        imageType = ((com.agencias.backend.model.Brand) entity).getImageType();
                    }
                    break;
                case "vehicle":
                    entity = em.find(com.agencias.backend.model.Vehicle.class, id);
                    if (entity != null) {
                        imageData = ((com.agencias.backend.model.Vehicle) entity).getImageData();
                        imageType = ((com.agencias.backend.model.Vehicle) entity).getImageType();
                    }
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse(400, "Tipo de entidad no válido")).build();
            }

            if (entity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(404, "Entidad no encontrada")).build();
            }

            if (imageData == null || imageData.length == 0) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(404, "Imagen no encontrada")).build();
            }

            // Determinar Content-Type
            String contentType = imageType != null ? imageType : "image/jpeg";

            return Response.ok(imageData)
                    .type(contentType)
                    .header("Cache-Control", "max-age=86400") // Cache por 1 día
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Error al obtener la imagen: " + e.getMessage())).build();
        } finally {
            em.close();
        }
    }

    /**
     * Endpoint para validar imagen en base64
     * POST /api/images/validate
     */
    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateImage(Map<String, String> payload) {
        try {
            String base64Data = payload.get("imageData");
            String imageType = payload.get("imageType");

            if (base64Data == null || base64Data.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(400, "No se proporcionó imagen")).build();
            }

            // Remover prefijo data:image/...;base64, si existe
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }

            // Decodificar base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Validar tamaño
            if (imageBytes.length > MAX_FILE_SIZE) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(400, "La imagen excede el tamaño máximo de 5MB")).build();
            }

            // Validar tipo
            if (imageType != null && !isValidImageType(imageType)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(400, "Formato de imagen no válido. Use: image/jpeg, image/png, image/gif, image/webp")).build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("size", imageBytes.length);
            response.put("sizeKB", imageBytes.length / 1024);

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Formato base64 inválido")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Error al validar la imagen: " + e.getMessage())).build();
        }
    }

    private boolean isValidImageType(String imageType) {
        return imageType.equals("image/jpeg") || imageType.equals("image/jpg") ||
               imageType.equals("image/png") || imageType.equals("image/gif") ||
               imageType.equals("image/webp");
    }
}
