package com.agencias.backend.service;

import com.agencias.backend.controller.dto.EngagementReporteDto;
import com.agencias.backend.controller.dto.ImportExportLogDto;
import com.agencias.backend.controller.dto.InventoryLogDto;
import com.agencias.backend.controller.dto.VentaLineaDto;
import com.agencias.backend.controller.dto.VentaReporteDto;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.ImportExportLog;
import com.agencias.backend.model.InventoryLog;
import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.Part;
import com.agencias.backend.model.PartEngagementLog;
import com.agencias.backend.repository.ImportExportLogRepository;
import com.agencias.backend.repository.InventoryLogRepository;
import com.agencias.backend.repository.OrderItemRepository;
import com.agencias.backend.repository.OrderRepository;
import com.agencias.backend.repository.PartEngagementLogRepository;
import com.agencias.backend.repository.PartRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteriaService {
    private final ImportExportLogRepository importExportLogRepo;
    private final InventoryLogRepository inventoryLogRepo;
    private final UserService userService;
    private final PartRepository partRepository;
    private final PartEngagementLogRepository engagementLogRepo;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public ReporteriaService(EntityManagerFactory emf) {
        this.importExportLogRepo = new ImportExportLogRepository(emf);
        this.inventoryLogRepo = new InventoryLogRepository(emf);
        this.userService = new UserService(emf);
        this.partRepository = new PartRepository(emf);
        this.engagementLogRepo = new PartEngagementLogRepository(emf);
        this.orderRepository = new OrderRepository(emf);
        this.orderItemRepository = new OrderItemRepository(emf);
    }

    /** Registra un evento de engagement (visto en detalle, agregado al carrito, consultado). Llamado por distribuidores. partId o partNumber debe estar presente. */
    public PartEngagementLog registerEvent(String eventType, Long partId, String partNumber, Long userId, String clientType, String source) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType es obligatorio");
        }
        Long resolvedPartId = partId;
        if (resolvedPartId == null && partNumber != null && !partNumber.isBlank()) {
            Part p = partRepository.findByPartNumber(partNumber.trim()).orElse(null);
            if (p != null) resolvedPartId = p.getPartId();
        }
        if (resolvedPartId == null) {
            throw new IllegalArgumentException("partId o partNumber (válido) es obligatorio");
        }
        PartEngagementLog log = new PartEngagementLog();
        log.setEventType(eventType.trim().toUpperCase());
        log.setPartId(resolvedPartId);
        log.setUserId(userId);
        log.setClientType(clientType != null && !clientType.isBlank() ? clientType.trim().toUpperCase() : null);
        log.setSource(source);
        return engagementLogRepo.save(log);
    }

    /** Reporte de ventas por período (pedidos con líneas). */
    public List<VentaReporteDto> reporteVentas(Date from, Date to) {
        Date fromDate = from != null ? from : startOfDay(0);
        Date toDate = to != null ? endOfDay(to) : endOfDay(new Date());
        List<OrderHeader> orders = orderRepository.findAllFiltered(null, null, fromDate, toDate);
        List<Long> userIds = orders.stream().map(OrderHeader::getUserId).distinct().toList();
        Map<Long, String> userNames = loadUserDisplayNames(userIds);

        List<VentaReporteDto> result = new ArrayList<>();
        for (OrderHeader order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            List<VentaLineaDto> lineas = new ArrayList<>();
            for (OrderItem item : items) {
                Part p = partRepository.findById(item.getPartId()).orElse(null);
                VentaLineaDto linea = new VentaLineaDto();
                linea.setPartId(item.getPartId());
                linea.setPartNumber(p != null ? p.getPartNumber() : null);
                linea.setPartTitle(p != null ? p.getTitle() : null);
                linea.setQty(item.getQty());
                linea.setUnitPrice(item.getUnitPrice());
                linea.setLineTotal(item.getLineTotal());
                lineas.add(linea);
            }
            VentaReporteDto dto = new VentaReporteDto();
            dto.setFecha(order.getCreatedAt());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setOrderId(order.getOrderId());
            dto.setUserId(order.getUserId());
            dto.setUserDisplayName(userNames.getOrDefault(order.getUserId(), "ID " + order.getUserId()));
            dto.setOrderType(order.getOrderType());
            dto.setTotal(order.getTotal());
            dto.setLineas(lineas);
            result.add(dto);
        }
        return result;
    }

    /** Reporte de repuestos por tipo de evento (consultados, vistos, carrito) agregado por partId. */
    public List<EngagementReporteDto> reporteEngagement(Date from, Date to, String eventType, Integer limit) {
        Date fromDate = from != null ? from : startOfDay(0);
        Date toDate = to != null ? endOfDay(to) : endOfDay(new Date());
        List<Object[]> rows = engagementLogRepo.countByPartIdAndEventType(fromDate, toDate, eventType);
        List<Long> partIds = rows.stream().map(r -> (Long) r[0]).toList();
        Map<Long, Part> parts = partIds.stream()
            .map(id -> partRepository.findById(id).orElse(null))
            .filter(p -> p != null)
            .collect(Collectors.toMap(Part::getPartId, p -> p));

        List<EngagementReporteDto> result = new ArrayList<>();
        int max = (limit != null && limit > 0) ? Math.min(limit, rows.size()) : rows.size();
        for (int i = 0; i < max; i++) {
            Object[] r = rows.get(i);
            Long partId = (Long) r[0];
            Long count = (Long) r[1];
            Part p = parts.get(partId);
            EngagementReporteDto dto = new EngagementReporteDto();
            dto.setPartId(partId);
            dto.setPartNumber(p != null ? p.getPartNumber() : null);
            dto.setPartTitle(p != null ? p.getTitle() : null);
            dto.setCount(count);
            dto.setFromDate(fromDate);
            dto.setToDate(toDate);
            result.add(dto);
        }
        return result;
    }

    private static Date startOfDay(int daysAgo) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -daysAgo);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date endOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    public List<ImportExportLogDto> listImportExportLog(Integer limit, String operation) {
        List<ImportExportLog> list = importExportLogRepo.findRecent(limit != null ? limit : 200, operation);
        List<Long> userIds = list.stream().map(ImportExportLog::getUserId).distinct().toList();
        Map<Long, String> userNames = loadUserDisplayNames(userIds);

        List<ImportExportLogDto> result = new ArrayList<>();
        for (ImportExportLog log : list) {
            ImportExportLogDto dto = new ImportExportLogDto();
            dto.setLogId(log.getLogId());
            dto.setUserId(log.getUserId());
            dto.setUserDisplayName(userNames.getOrDefault(log.getUserId(), "ID " + log.getUserId()));
            dto.setCreatedAt(log.getCreatedAt());
            dto.setOperation(log.getOperation());
            dto.setFileName(log.getFileName());
            dto.setSuccessCount(log.getSuccessCount());
            dto.setErrorCount(log.getErrorCount());
            dto.setDetail(log.getDetail());
            result.add(dto);
        }
        return result;
    }

    public List<InventoryLogDto> listInventoryLog(Integer limit, Long partId, Long userId) {
        List<InventoryLog> list = inventoryLogRepo.findRecent(limit != null ? limit : 200, partId, userId);
        List<Long> userIds = list.stream().map(InventoryLog::getUserId).distinct().toList();
        List<Long> partIds = list.stream().map(InventoryLog::getPartId).distinct().toList();
        Map<Long, String> userNames = loadUserDisplayNames(userIds);
        Map<Long, Part> parts = partIds.stream()
            .map(id -> partRepository.findById(id).orElse(null))
            .filter(p -> p != null)
            .collect(Collectors.toMap(Part::getPartId, p -> p));

        List<InventoryLogDto> result = new ArrayList<>();
        for (InventoryLog log : list) {
            InventoryLogDto dto = new InventoryLogDto();
            dto.setLogId(log.getLogId());
            dto.setPartId(log.getPartId());
            Part p = parts.get(log.getPartId());
            dto.setPartTitle(p != null ? p.getTitle() : null);
            dto.setPartNumber(p != null ? p.getPartNumber() : null);
            dto.setUserId(log.getUserId());
            dto.setUserDisplayName(userNames.getOrDefault(log.getUserId(), "ID " + log.getUserId()));
            dto.setCreatedAt(log.getCreatedAt());
            dto.setQuantityAdded(log.getQuantityAdded());
            dto.setPreviousQuantity(log.getPreviousQuantity());
            dto.setNewQuantity(log.getNewQuantity());
            result.add(dto);
        }
        return result;
    }

    private Map<Long, String> loadUserDisplayNames(List<Long> userIds) {
        Map<Long, String> map = new java.util.HashMap<>();
        for (Long uid : userIds) {
            AppUser u = userService.getById(uid);
            if (u != null) {
                String name = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getEmail();
                map.put(uid, name);
            }
        }
        return map;
    }
}
