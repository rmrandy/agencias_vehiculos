package com.agencias.backend.controller;

import com.agencias.backend.controller.dto.EngagementReporteDto;
import com.agencias.backend.controller.dto.ImportExportLogDto;
import com.agencias.backend.controller.dto.InventoryLogDto;
import com.agencias.backend.controller.dto.VentaReporteDto;
import com.agencias.backend.model.PartEngagementLog;
import com.agencias.backend.service.ReporteriaService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.agencias.backend.config.DatabaseConfig;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Reportería: log de operaciones (import/export e inventario) y reportes de ventas/engagement.
 * Los distribuidores envían eventos (visto en detalle, agregado al carrito, consultado).
 */
@Path("/reporteria")
@jakarta.inject.Singleton
public class ReporteriaResource {

    private final ReporteriaService service;

    public ReporteriaResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new ReporteriaService(emf);
    }

    /**
     * Recibir evento desde distribuidor: repuesto visto en detalle.
     * POST /api/reporteria/visto-detalle
     * Body: { partId, userId?, clientType?, source? }
     */
    @POST
    @Path("/visto-detalle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registroVistoDetalle(Map<String, Object> body) {
        try {
            PartEngagementLog log = service.registerEvent(
                PartEngagementLog.EVENT_VIEW_DETAIL,
                longOrNull(body, "partId"),
                stringOrNull(body, "partNumber"),
                longOrNull(body, "userId"),
                stringOrNull(body, "clientType"),
                stringOrNull(body, "source"));
            return Response.status(Response.Status.CREATED).entity(Map.of("logId", log.getLogId(), "eventType", log.getEventType())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Recibir evento desde distribuidor: repuesto agregado al carrito (sin compra).
     * POST /api/reporteria/agregado-carrito
     * Body: { partId, userId?, clientType?, source? }
     */
    @POST
    @Path("/agregado-carrito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registroAgregadoCarrito(Map<String, Object> body) {
        try {
            PartEngagementLog log = service.registerEvent(
                PartEngagementLog.EVENT_ADD_TO_CART,
                longOrNull(body, "partId"),
                stringOrNull(body, "partNumber"),
                longOrNull(body, "userId"),
                stringOrNull(body, "clientType"),
                stringOrNull(body, "source"));
            return Response.status(Response.Status.CREATED).entity(Map.of("logId", log.getLogId(), "eventType", log.getEventType())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Recibir evento: repuesto consultado (búsqueda/listado).
     * POST /api/reporteria/consultado
     * Body: { partId, userId?, clientType?, source? }
     */
    @POST
    @Path("/consultado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registroConsultado(Map<String, Object> body) {
        try {
            PartEngagementLog log = service.registerEvent(
                PartEngagementLog.EVENT_SEARCH,
                longOrNull(body, "partId"),
                stringOrNull(body, "partNumber"),
                longOrNull(body, "userId"),
                stringOrNull(body, "clientType"),
                stringOrNull(body, "source"));
            return Response.status(Response.Status.CREATED).entity(Map.of("logId", log.getLogId(), "eventType", log.getEventType())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Reporte de ventas por período.
     * GET /api/reporteria/ventas?from=yyyy-MM-dd&to=yyyy-MM-dd
     */
    @GET
    @Path("/ventas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteVentas(@QueryParam("from") String fromStr, @QueryParam("to") String toStr) {
        try {
            Date from = parseDate(fromStr);
            Date to = parseDate(toStr);
            List<VentaReporteDto> list = service.reporteVentas(from, to);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Reporte repuestos consultados (búsquedas) en el período.
     * GET /api/reporteria/repuestos-consultados?from=&to=&limit=
     */
    @GET
    @Path("/repuestos-consultados")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteRepuestosConsultados(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr,
            @QueryParam("limit") Integer limit) {
        try {
            Date from = parseDate(fromStr);
            Date to = parseDate(toStr);
            List<EngagementReporteDto> list = service.reporteEngagement(from, to, PartEngagementLog.EVENT_SEARCH, limit);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Reporte repuestos vistos en detalle en el período.
     * GET /api/reporteria/repuestos-vistos?from=&to=&limit=
     */
    @GET
    @Path("/repuestos-vistos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteRepuestosVistos(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr,
            @QueryParam("limit") Integer limit) {
        try {
            Date from = parseDate(fromStr);
            Date to = parseDate(toStr);
            List<EngagementReporteDto> list = service.reporteEngagement(from, to, PartEngagementLog.EVENT_VIEW_DETAIL, limit);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Reporte repuestos agregados al carrito en el período.
     * GET /api/reporteria/repuestos-carrito?from=&to=&limit=
     */
    @GET
    @Path("/repuestos-carrito")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteRepuestosCarrito(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr,
            @QueryParam("limit") Integer limit) {
        try {
            Date from = parseDate(fromStr);
            Date to = parseDate(toStr);
            List<EngagementReporteDto> list = service.reporteEngagement(from, to, PartEngagementLog.EVENT_ADD_TO_CART, limit);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    private static Long longOrNull(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static String stringOrNull(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : v.toString().trim();
    }

    private static Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return java.text.SimpleDateFormat.getDateInstance().parse(s);
        } catch (Exception e) {
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * GET /api/reporteria/import-export
     * Lista log de operaciones de importación/exportación.
     * Query: limit (default 200), operation (EXPORT, IMPORT, IMPORT_INVENTORY)
     */
    @GET
    @Path("/import-export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listImportExport(
            @QueryParam("limit") Integer limit,
            @QueryParam("operation") String operation) {
        try {
            List<ImportExportLogDto> list = service.listImportExportLog(limit, operation);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * GET /api/reporteria/inventario
     * Lista log de altas de inventario.
     * Query: limit (default 200), partId, userId
     */
    @GET
    @Path("/inventario")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listInventario(
            @QueryParam("limit") Integer limit,
            @QueryParam("partId") Long partId,
            @QueryParam("userId") Long userId) {
        try {
            List<InventoryLogDto> list = service.listInventoryLog(limit, partId, userId);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }
}
