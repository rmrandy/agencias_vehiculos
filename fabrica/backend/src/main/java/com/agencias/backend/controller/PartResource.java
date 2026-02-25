package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.Part;
import com.agencias.backend.service.MailService;
import com.agencias.backend.service.PartService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Path("/repuestos")
@jakarta.inject.Singleton
public class PartResource {
    private final PartService service;

    public PartResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new PartService(emf, new MailService());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Map<String, Object> body) {
        try {
            Long categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
            Long brandId = body.get("brandId") != null ? ((Number) body.get("brandId")).longValue() : null;
            String partNumber = (String) body.get("partNumber");
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            BigDecimal weightLb = body.get("weightLb") != null ? new BigDecimal(body.get("weightLb").toString()) : null;
            BigDecimal price = body.get("price") != null ? new BigDecimal(body.get("price").toString()) : null;
            Integer stockQuantity = body.get("stockQuantity") != null ? ((Number) body.get("stockQuantity")).intValue() : 0;
            Integer lowStockThreshold = body.get("lowStockThreshold") != null ? ((Number) body.get("lowStockThreshold")).intValue() : 5;
            
            // Procesar imagen si existe
            byte[] imageData = null;
            String imageType = null;
            if (body.containsKey("imageData") && body.get("imageData") != null) {
                String base64Data = (String) body.get("imageData");
                imageType = (String) body.get("imageType");
                
                // Remover prefijo data:image/...;base64, si existe
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                imageData = Base64.getDecoder().decode(base64Data);
            }
            
            Part p = service.create(categoryId, brandId, partNumber, title, description, weightLb, price, stockQuantity, lowStockThreshold);
            
            // Agregar imagen si existe
            if (imageData != null) {
                p = service.updateImage(p.getPartId(), imageData, imageType);
            }
            
            return Response.status(Response.Status.CREATED).entity(p).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("categoryId") Long categoryId, @QueryParam("brandId") Long brandId) {
        try {
            List<Part> list;
            if (categoryId != null) {
                list = service.listByCategory(categoryId);
            } else if (brandId != null) {
                list = service.listByBrand(brandId);
            } else {
                list = service.listAll();
            }
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Servicio de búsqueda. Recibe nombre, descripción, especificaciones.
     * Responde: lista de repuestos con código (partNumber).
     * GET /api/repuestos/busqueda?nombre=&descripcion=&especificaciones=
     */
    @GET
    @Path("/busqueda")
    @Produces(MediaType.APPLICATION_JSON)
    public Response busqueda(
            @QueryParam("nombre") String nombre,
            @QueryParam("descripcion") String descripcion,
            @QueryParam("especificaciones") String especificaciones) {
        try {
            List<Part> list = service.search(nombre, descripcion, especificaciones);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Exportar repuestos a JSON.
     * GET /api/repuestos/export?userId=
     */
    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response export(@QueryParam("userId") Long userId) {
        try {
            List<Part> list = service.exportRepuestos(userId);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Importar repuestos desde JSON (sobreescribe datos).
     * POST /api/repuestos/import
     * Body: { userId?, fileName?, items: [ { partNumber, title?, description?, ... } ] }
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRepuestos(Map<String, Object> body) {
        try {
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            String fileName = (String) body.get("fileName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null) items = Collections.emptyList();
            Map<String, Object> result = service.importRepuestos(items, userId, fileName);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Carga masiva de inventario por JSON.
     * POST /api/repuestos/import-inventario
     * Body: { userId?, fileName?, items: [ { partNumber, stockQuantity } ] }
     */
    @POST
    @Path("/import-inventario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importInventario(Map<String, Object> body) {
        try {
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            String fileName = (String) body.get("fileName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null) items = Collections.emptyList();
            Map<String, Object> result = service.importInventario(items, userId, fileName);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        Part p = service.getById(id);
        if (p == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Repuesto no encontrado")).build();
        }
        return Response.ok(p).build();
    }

    @GET
    @Path("/numero/{partNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByPartNumber(@PathParam("partNumber") String partNumber) {
        Part p = service.getByPartNumber(partNumber);
        if (p == null) {
            return Response.status(404).entity(new ErrorResponse(404, "Repuesto no encontrado")).build();
        }
        return Response.ok(p).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            Long categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
            Long brandId = body.get("brandId") != null ? ((Number) body.get("brandId")).longValue() : null;
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            BigDecimal weightLb = body.get("weightLb") != null ? new BigDecimal(body.get("weightLb").toString()) : null;
            BigDecimal price = body.get("price") != null ? new BigDecimal(body.get("price").toString()) : null;
            Integer active = body.get("active") != null ? ((Number) body.get("active")).intValue() : null;
            Integer stockQuantity = body.get("stockQuantity") != null ? ((Number) body.get("stockQuantity")).intValue() : null;
            Integer lowStockThreshold = body.get("lowStockThreshold") != null ? ((Number) body.get("lowStockThreshold")).intValue() : null;
            
            // Actualizar datos básicos
            Part p = service.update(id, categoryId, brandId, title, description, weightLb, price, active);
            
            // Actualizar inventario si se proporcionó
            if (stockQuantity != null || lowStockThreshold != null) {
                p = service.updateInventory(id, stockQuantity, lowStockThreshold);
            }
            
            // Actualizar imagen si se proporcionó
            if (body.containsKey("imageData") && body.get("imageData") != null) {
                String base64Data = (String) body.get("imageData");
                String imageType = (String) body.get("imageType");
                
                // Remover prefijo data:image/...;base64, si existe
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                byte[] imageData = Base64.getDecoder().decode(base64Data);
                p = service.updateImage(id, imageData, imageType);
            }
            
            return Response.ok(p).build();
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

    /**
     * Actualizar inventario de un repuesto
     * PUT /api/repuestos/{id}/inventario
     */
    @PUT
    @Path("/{id}/inventario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateInventory(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            Integer stockQuantity = body.get("stockQuantity") != null ? ((Number) body.get("stockQuantity")).intValue() : null;
            Integer lowStockThreshold = body.get("lowStockThreshold") != null ? ((Number) body.get("lowStockThreshold")).intValue() : null;
            
            Part p = service.updateInventory(id, stockQuantity, lowStockThreshold);
            return Response.ok(p).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Alta de inventario: agregar unidades y registrar en log.
     * POST /api/repuestos/{id}/inventario/alta
     * Body: { userId, cantidad }
     */
    @POST
    @Path("/{id}/inventario/alta")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addInventory(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            Integer cantidad = body.get("cantidad") != null ? ((Number) body.get("cantidad")).intValue() : null;
            if (userId == null) {
                return Response.status(400).entity(new ErrorResponse(400, "userId es obligatorio")).build();
            }
            if (cantidad == null || cantidad <= 0) {
                return Response.status(400).entity(new ErrorResponse(400, "cantidad debe ser mayor a cero")).build();
            }
            Part p = service.addInventory(id, userId, cantidad);
            return Response.ok(p).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Verificar disponibilidad de stock
     * GET /api/repuestos/{id}/disponibilidad?cantidad=5
     */
    @GET
    @Path("/{id}/disponibilidad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAvailability(@PathParam("id") Long id, @QueryParam("cantidad") Integer cantidad) {
        try {
            if (cantidad == null || cantidad <= 0) {
                return Response.status(400).entity(new ErrorResponse(400, "La cantidad debe ser mayor a cero")).build();
            }
            
            boolean available = service.checkAvailability(id, cantidad);
            return Response.ok(Map.of("available", available, "quantity", cantidad)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(new ErrorResponse(404, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }
}
