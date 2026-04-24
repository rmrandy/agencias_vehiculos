package com.agencias.backend.service;

import com.agencias.backend.controller.dto.EngagementReporteDto;
import com.agencias.backend.controller.dto.ImportExportLogDto;
import com.agencias.backend.controller.dto.InventoryLogDto;
import com.agencias.backend.controller.dto.MasVendidoDto;
import com.agencias.backend.controller.dto.PedidosPorOrigenDto;
import com.agencias.backend.controller.dto.VentaDiariaDto;
import com.agencias.backend.controller.dto.VentaLineaDto;
import com.agencias.backend.controller.dto.VentaReporteDto;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.ImportExportLog;
import com.agencias.backend.model.InventoryLog;
import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.OrderStatusHistory;
import com.agencias.backend.model.Part;
import com.agencias.backend.model.PartEngagementLog;
import com.agencias.backend.repository.ImportExportLogRepository;
import com.agencias.backend.repository.InventoryLogRepository;
import com.agencias.backend.repository.OrderItemRepository;
import com.agencias.backend.repository.OrderRepository;
import com.agencias.backend.repository.PartEngagementLogRepository;
import com.agencias.backend.repository.PartRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteriaService {
    private final EntityManagerFactory emf;
    private final ImportExportLogRepository importExportLogRepo;
    private final InventoryLogRepository inventoryLogRepo;
    private final UserService userService;
    private final PartRepository partRepository;
    private final PartEngagementLogRepository engagementLogRepo;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public ReporteriaService(EntityManagerFactory emf) {
        this.emf = emf;
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
        List<OrderHeader> orders = orderRepository.findAllFiltered(null, null, fromDate, toDate, null);
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

    /**
     * Repuestos más vendidos (suma de cantidades) en pedidos cuyo último estado no es CANCELLED.
     */
    public List<MasVendidoDto> reporteMasVendidos(Date from, Date to, Integer limit) {
        Date fromDate = from != null ? from : startOfDay(90);
        Date toDate = to != null ? endOfDay(to) : endOfDay(new Date());
        List<OrderHeader> orders = orderRepository.findAllFiltered(null, null, fromDate, toDate, null);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(OrderHeader::getOrderId).toList();
        Map<Long, OrderStatusHistory> latestByOrder = loadLatestStatusByOrderId(orderIds);
        List<Long> activeOrderIds = orders.stream()
            .map(OrderHeader::getOrderId)
            .filter(oid -> {
                OrderStatusHistory st = latestByOrder.get(oid);
                return st == null || !"CANCELLED".equalsIgnoreCase(st.getStatus());
            })
            .toList();
        if (activeOrderIds.isEmpty()) {
            return List.of();
        }
        List<OrderItem> items = orderItemRepository.findByOrderIds(activeOrderIds);
        Map<Long, Long> qtyByPart = new HashMap<>();
        Map<Long, BigDecimal> importeByPart = new HashMap<>();
        for (OrderItem oi : items) {
            Long pid = oi.getPartId();
            if (pid == null) {
                continue;
            }
            qtyByPart.merge(pid, (long) oi.getQty(), Long::sum);
            importeByPart.merge(pid, oi.getLineTotal() != null ? oi.getLineTotal() : BigDecimal.ZERO, BigDecimal::add);
        }
        int max = (limit != null && limit > 0) ? limit : 30;
        List<Long> topPartIds = qtyByPart.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(max)
            .map(Map.Entry::getKey)
            .toList();

        List<MasVendidoDto> out = new ArrayList<>();
        for (Long partId : topPartIds) {
            Part p = partRepository.findById(partId).orElse(null);
            MasVendidoDto dto = new MasVendidoDto();
            dto.setPartId(partId);
            dto.setPartNumber(p != null ? p.getPartNumber() : null);
            dto.setPartTitle(p != null ? p.getTitle() : null);
            dto.setTotalQty(qtyByPart.getOrDefault(partId, 0L));
            dto.setTotalImporte(importeByPart.getOrDefault(partId, BigDecimal.ZERO));
            out.add(dto);
        }
        return out;
    }

    /** Pedidos por día (conteo e importe total), excluye pedidos cancelados al cierre del período. */
    public List<VentaDiariaDto> reporteVentasPorDia(Date from, Date to) {
        Date fromDate = from != null ? from : startOfDay(90);
        Date toDate = to != null ? endOfDay(to) : endOfDay(new Date());
        List<OrderHeader> orders = orderRepository.findAllFiltered(null, null, fromDate, toDate, null);
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, OrderStatusHistory> latestByOrder = loadLatestStatusByOrderId(
            orders.stream().map(OrderHeader::getOrderId).toList());
        Map<Date, Long> countByDay = new HashMap<>();
        Map<Date, BigDecimal> sumByDay = new HashMap<>();
        for (OrderHeader oh : orders) {
            OrderStatusHistory st = latestByOrder.get(oh.getOrderId());
            if (st != null && "CANCELLED".equalsIgnoreCase(st.getStatus())) {
                continue;
            }
            Date day = startOfDayForDate(oh.getCreatedAt());
            countByDay.merge(day, 1L, Long::sum);
            sumByDay.merge(day, oh.getTotal() != null ? oh.getTotal() : BigDecimal.ZERO, BigDecimal::add);
        }
        List<VentaDiariaDto> list = new ArrayList<>();
        for (Map.Entry<Date, Long> e : countByDay.entrySet()) {
            VentaDiariaDto dto = new VentaDiariaDto();
            dto.setFecha(e.getKey());
            dto.setPedidoCount(e.getValue());
            dto.setTotalImporte(sumByDay.getOrDefault(e.getKey(), BigDecimal.ZERO));
            list.add(dto);
        }
        list.sort(Comparator.comparing(VentaDiariaDto::getFecha));
        return list;
    }

    /** Pedidos agrupados por origen (FABRICA_WEB vs DISTRIBUIDORA), excluye cancelados. */
    public List<PedidosPorOrigenDto> reportePedidosPorOrigen(Date from, Date to) {
        Date fromDate = from != null ? from : startOfDay(90);
        Date toDate = to != null ? endOfDay(to) : endOfDay(new Date());
        List<OrderHeader> orders = orderRepository.findAllFiltered(null, null, fromDate, toDate, null);
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, OrderStatusHistory> latestByOrder = loadLatestStatusByOrderId(
            orders.stream().map(OrderHeader::getOrderId).toList());
        Map<String, Long> countByOrigin = new HashMap<>();
        Map<String, BigDecimal> sumByOrigin = new HashMap<>();
        for (OrderHeader oh : orders) {
            OrderStatusHistory st = latestByOrder.get(oh.getOrderId());
            if (st != null && "CANCELLED".equalsIgnoreCase(st.getStatus())) {
                continue;
            }
            String origin = oh.getOrderOrigin() != null && !oh.getOrderOrigin().isBlank()
                ? oh.getOrderOrigin()
                : "FABRICA_WEB";
            countByOrigin.merge(origin, 1L, Long::sum);
            sumByOrigin.merge(origin, oh.getTotal() != null ? oh.getTotal() : BigDecimal.ZERO, BigDecimal::add);
        }
        List<PedidosPorOrigenDto> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : countByOrigin.entrySet()) {
            PedidosPorOrigenDto dto = new PedidosPorOrigenDto();
            dto.setOrderOrigin(e.getKey());
            dto.setPedidoCount(e.getValue());
            dto.setTotalImporte(sumByOrigin.getOrDefault(e.getKey(), BigDecimal.ZERO));
            list.add(dto);
        }
        list.sort(Comparator.comparing(PedidosPorOrigenDto::getOrderOrigin));
        return list;
    }

    private Map<Long, OrderStatusHistory> loadLatestStatusByOrderId(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        EntityManager em = emf.createEntityManager();
        try {
            List<OrderStatusHistory> all = em.createQuery(
                "SELECT h FROM OrderStatusHistory h WHERE h.orderId IN :ids",
                OrderStatusHistory.class)
                .setParameter("ids", orderIds)
                .getResultList();
            Map<Long, OrderStatusHistory> map = new HashMap<>();
            for (OrderStatusHistory h : all) {
                OrderStatusHistory cur = map.get(h.getOrderId());
                if (cur == null) {
                    map.put(h.getOrderId(), h);
                    continue;
                }
                Date hc = h.getChangedAt();
                Date cc = cur.getChangedAt();
                if (hc != null && (cc == null || hc.after(cc))) {
                    map.put(h.getOrderId(), h);
                } else if (hc == null && cc == null && h.getStatusId() != null && cur.getStatusId() != null
                    && h.getStatusId() > cur.getStatusId()) {
                    map.put(h.getOrderId(), h);
                }
            }
            return map;
        } finally {
            em.close();
        }
    }

    private static Date startOfDayForDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d != null ? d : new Date());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
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
