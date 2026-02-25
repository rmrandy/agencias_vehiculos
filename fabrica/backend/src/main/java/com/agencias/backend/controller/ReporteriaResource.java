package com.agencias.backend.controller;

import com.agencias.backend.controller.dto.ImportExportLogDto;
import com.agencias.backend.controller.dto.InventoryLogDto;
import com.agencias.backend.service.ReporteriaService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.agencias.backend.config.DatabaseConfig;

import java.util.List;

/**
 * Reportería: log de operaciones (import/export e inventario).
 * Quién, cuándo, archivo usado, cuántos exitosos, cuántos con error.
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
