package com.agencias.backend.service;

import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.OrderStatusHistory;
import com.agencias.backend.model.Part;
import com.agencias.backend.repository.AppUserRepository;
import com.agencias.backend.repository.OrderRepository;
import com.agencias.backend.repository.OrderItemRepository;
import com.agencias.backend.repository.OrderStatusRepository;
import com.agencias.backend.repository.EnterpriseProfileRepository;
import com.agencias.backend.repository.PartRepository;
import com.agencias.backend.model.EnterpriseProfile;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Flujo: Iniciada → Preparación del pedido → Enviado → Entregado. No se puede volver atrás. */
public class OrderService {

    private static final List<String> STATUS_FLOW = List.of(
        "INITIATED",   // Iniciada
        "PREPARING",   // Preparación del pedido (nombre en BD)
        "SHIPPED",     // Enviado
        "DELIVERED"    // Entregado
    );
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static int statusIndex(String status) {
        if (status == null) return -1;
        String u = status.toUpperCase();
        if (STATUS_CANCELLED.equals(u)) return STATUS_FLOW.size();
        for (int i = 0; i < STATUS_FLOW.size(); i++) {
            if (STATUS_FLOW.get(i).equals(u)) return i;
        }
        if ("CONFIRMED".equals(u) || "IN_PREPARATION".equals(u)) return 1; // legacy
        return -1;
    }
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final OrderStatusRepository statusRepo;
    private final PartRepository partRepo;
    private final AppUserRepository userRepo;
    private final EnterpriseProfileRepository enterpriseProfileRepo;
    private final PartService partService;
    private final MailService mailService;

    public OrderService(EntityManagerFactory emf) {
        this.orderRepo = new OrderRepository(emf);
        this.itemRepo = new OrderItemRepository(emf);
        this.statusRepo = new OrderStatusRepository(emf);
        this.partRepo = new PartRepository(emf);
        this.userRepo = new AppUserRepository(emf);
        this.enterpriseProfileRepo = new EnterpriseProfileRepository(emf);
        this.mailService = new MailService();
        this.partService = new PartService(emf, this.mailService);
    }

    public OrderHeader createOrder(Long userId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un artículo");
        }

        // 1. Validar disponibilidad de stock para todos los items
        for (Map<String, Object> item : items) {
            Long partId = ((Number) item.get("partId")).longValue();
            Integer qty = ((Number) item.get("qty")).intValue();

            Part part = partRepo.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado: " + partId));

            if (!partService.checkAvailability(partId, qty)) {
                throw new IllegalArgumentException("Stock insuficiente para: " + part.getTitle() + 
                    " (disponible: " + (part.getStockQuantity() - part.getReservedQuantity()) + ", solicitado: " + qty + ")");
            }
        }

        // 2. Reservar stock para todos los items
        for (Map<String, Object> item : items) {
            Long partId = ((Number) item.get("partId")).longValue();
            Integer qty = ((Number) item.get("qty")).intValue();

            if (!partService.reserveStock(partId, qty)) {
                // Si falla la reserva, liberar todo lo reservado hasta ahora
                rollbackReservations(items);
                throw new IllegalArgumentException("No se pudo reservar el stock");
            }
        }

        // 3. Calcular totales
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Long partId = ((Number) item.get("partId")).longValue();
            Integer qty = ((Number) item.get("qty")).intValue();

            Part part = partRepo.findById(partId).get();
            BigDecimal lineTotal = part.getPrice().multiply(new BigDecimal(qty));
            subtotal = subtotal.add(lineTotal);
        }

        // Descuento empresarial si aplica
        BigDecimal total = subtotal;
        String orderType = "WEB";
        java.util.Optional<EnterpriseProfile> enterpriseOpt = enterpriseProfileRepo.findByUserId(userId);
        if (enterpriseOpt.isPresent()) {
            EnterpriseProfile ep = enterpriseOpt.get();
            if (ep.getDiscountPercent() != null && ep.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = subtotal.multiply(ep.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                total = subtotal.subtract(discount);
                orderType = "ENTERPRISE_API";
            }
        }

        // 4. Crear orden
        OrderHeader order = new OrderHeader();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setOrderType(orderType);
        order.setSubtotal(subtotal);
        order.setShippingTotal(BigDecimal.ZERO); // Por ahora sin envío
        order.setTotal(total);
        order.setCurrency("USD");

        order = orderRepo.save(order);

        // 5. Crear items y confirmar venta (reducir stock)
        for (Map<String, Object> itemData : items) {
            Long partId = ((Number) itemData.get("partId")).longValue();
            Integer qty = ((Number) itemData.get("qty")).intValue();

            Part part = partRepo.findById(partId).get();

            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setPartId(partId);
            item.setQty(qty);
            item.setUnitPrice(part.getPrice());
            item.setLineTotal(part.getPrice().multiply(new BigDecimal(qty)));

            itemRepo.save(item);

            // Confirmar venta (reducir stock y cantidad reservada)
            partService.confirmSale(partId, qty);
        }

        // 6. Crear estado inicial
        OrderStatusHistory status = new OrderStatusHistory();
        status.setOrderId(order.getOrderId());
        status.setStatus("INITIATED");
        status.setCommentText("Pedido creado");
        status.setChangedByUserId(userId);

        statusRepo.save(status);

        return order;
    }

    /**
     * Envía correo de confirmación al comprador con el detalle del pedido.
     */
    public void sendOrderConfirmationEmail(Long orderId) {
        OrderHeader order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        List<OrderItem> items = itemRepo.findByOrderId(orderId);
        AppUser user = userRepo.findById(order.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null) return;

        mailService.sendOrderConfirmation(
            user.getEmail(),
            user.getFullName(),
            order,
            items,
            partId -> partRepo.findById(partId)
        );
    }

    /**
     * Liberar reservas en caso de error
     */
    private void rollbackReservations(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            try {
                Long partId = ((Number) item.get("partId")).longValue();
                Integer qty = ((Number) item.get("qty")).intValue();
                partService.releaseStock(partId, qty);
            } catch (Exception e) {
                // Ignorar errores en rollback
            }
        }
    }

    public List<OrderHeader> getUserOrders(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    public OrderHeader getOrderById(Long orderId) {
        return orderRepo.findById(orderId).orElse(null);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return itemRepo.findByOrderId(orderId);
    }

    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return statusRepo.findByOrderId(orderId);
    }

    public OrderStatusHistory getLatestStatus(Long orderId) {
        return statusRepo.findLatestByOrderId(orderId);
    }

    /**
     * Actualiza el estado del pedido. No permite volver a un estado anterior.
     * Para estado SHIPPED se pueden indicar trackingNumber y etaDays (o usar default configurable).
     * Envía correo al cliente con la actualización (comentario, tracking y ETA si aplica).
     */
    public void updateOrderStatus(Long orderId, String status, String comment, Long changedByUserId,
                                   String trackingNumber, Integer etaDays) {
        OrderStatusHistory current = statusRepo.findLatestByOrderId(orderId);
        if (current == null) {
            throw new IllegalArgumentException("Pedido no encontrado");
        }
        int currentIdx = statusIndex(current.getStatus());
        int newIdx = statusIndex(status);
        if (newIdx < 0) {
            throw new IllegalArgumentException("Estado no válido: " + status + ". Use: " + String.join(", ", STATUS_FLOW) + " o " + STATUS_CANCELLED);
        }
        String newStatusUpper = status.toUpperCase();
        if (STATUS_CANCELLED.equals(newStatusUpper)) {
            if (currentIdx >= STATUS_FLOW.size() - 1) {
                throw new IllegalArgumentException("No se puede cancelar un pedido ya entregado");
            }
        } else if (newIdx <= currentIdx) {
            throw new IllegalArgumentException("No se puede regresar a un estado anterior. Estado actual: " + current.getStatus());
        }

        int effectiveEta = etaDays != null ? etaDays : getDefaultEtaDays();
        if ("SHIPPED".equals(newStatusUpper) && effectiveEta < 0) {
            effectiveEta = 5; // fallback si no hay config
        }

        OrderStatusHistory statusHistory = new OrderStatusHistory();
        statusHistory.setOrderId(orderId);
        statusHistory.setStatus(newStatusUpper);
        statusHistory.setCommentText(comment);
        statusHistory.setChangedByUserId(changedByUserId);
        if ("SHIPPED".equals(newStatusUpper)) {
            statusHistory.setTrackingNumber(trackingNumber);
            statusHistory.setEtaDays(effectiveEta > 0 ? effectiveEta : null);
        }
        statusRepo.save(statusHistory);

        try {
            OrderHeader order = orderRepo.findById(orderId).orElse(null);
            if (order != null) {
                AppUser user = userRepo.findById(order.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    mailService.sendOrderStatusUpdate(
                        user.getEmail(),
                        user.getFullName(),
                        order.getOrderNumber(),
                        statusHistory.getStatus(),
                        comment,
                        statusHistory.getTrackingNumber(),
                        statusHistory.getEtaDays()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error enviando correo de actualización de estado: " + e.getMessage());
        }
    }

    private int getDefaultEtaDays() {
        String v = System.getenv("ORDER_DEFAULT_ETA_DAYS");
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) { }
        }
        try {
            java.util.Properties p = new java.util.Properties();
            try (java.io.InputStream in = OrderService.class.getResourceAsStream("/application.properties")) {
                if (in != null) p.load(in);
            }
            String prop = p.getProperty("order.default.eta.days");
            if (prop != null && !prop.isBlank()) return Integer.parseInt(prop.trim());
        } catch (Exception ignored) { }
        return 5;
    }

    public List<String> getValidStatusFlow() {
        return STATUS_FLOW;
    }

    public List<OrderHeader> getAllOrders() {
        return orderRepo.findAll();
    }

    public List<OrderHeader> getAllOrdersFiltered(String status, Long userId, java.util.Date fromDate, java.util.Date toDate) {
        return orderRepo.findAllFiltered(status, userId, fromDate, toDate);
    }

    private String generateOrderNumber() {
        // Formato: ORD-timestamp
        return "ORD-" + Instant.now().toEpochMilli();
    }
}
