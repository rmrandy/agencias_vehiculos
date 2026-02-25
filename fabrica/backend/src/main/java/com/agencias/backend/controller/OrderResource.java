package com.agencias.backend.controller;

import com.agencias.backend.config.DatabaseConfig;
import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.OrderStatusHistory;
import com.agencias.backend.service.OrderService;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Path("/pedidos")
@Singleton
public class OrderResource {
    private final OrderService service;

    public OrderResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new OrderService(emf);
    }

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");

    /**
     * Crear un nuevo pedido. Opcionalmente incluye datos de pago (simulación) para validar tarjeta y enviar correo.
     * POST /api/pedidos
     * Body: { userId, items, payment?: { cardNumber, expiryMonth, expiryYear } }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(Map<String, Object> body) {
        try {
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            if (userId == null) {
                return Response.status(400).entity(new ErrorResponse(400, "userId es obligatorio")).build();
            }

            // Validación de pago (simulación) si se envía payment
            @SuppressWarnings("unchecked")
            Map<String, Object> payment = (Map<String, Object>) body.get("payment");
            if (payment != null) {
                String cardError = validateCard(payment);
                if (cardError != null) {
                    return Response.status(400).entity(new ErrorResponse(400, cardError)).build();
                }
            }

            OrderHeader order = service.createOrder(userId, items);

            // Enviar correo de confirmación si se enviaron datos de pago
            if (payment != null) {
                try {
                    service.sendOrderConfirmationEmail(order.getOrderId());
                } catch (Exception e) {
                    System.err.println("Error enviando correo de confirmación: " + e.getMessage());
                    // No fallar el pedido si el correo falla
                }
            }

            return Response.status(Response.Status.CREATED).entity(order).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Valida número de tarjeta (solo dígitos, 13-19 caracteres) y fecha de vencimiento (MM/YY en el futuro).
     * @return mensaje de error o null si es válido
     */
    private String validateCard(Map<String, Object> payment) {
        String cardNumber = (String) payment.get("cardNumber");
        if (cardNumber == null || cardNumber.isBlank()) {
            return "El número de tarjeta es obligatorio";
        }
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return "El número de tarjeta debe tener entre 13 y 19 dígitos";
        }
        if (!ONLY_DIGITS.matcher(digits).matches()) {
            return "El número de tarjeta solo puede contener dígitos";
        }

        Object mmObj = payment.get("expiryMonth");
        Object yyObj = payment.get("expiryYear");
        if (mmObj == null || yyObj == null) {
            return "La fecha de vencimiento (mes y año) es obligatoria";
        }
        int month = ((Number) mmObj).intValue();
        int year = ((Number) yyObj).intValue();
        if (year < 100) {
            year += 2000; // 25 -> 2025
        }
        if (month < 1 || month > 12) {
            return "El mes de vencimiento debe ser entre 01 y 12";
        }
        YearMonth expiry = YearMonth.of(year, month);
        if (expiry.isBefore(YearMonth.now())) {
            return "La tarjeta está vencida";
        }
        return null;
    }

    /**
     * Obtener pedidos de un usuario
     * GET /api/pedidos/usuario/{userId}
     */
    @GET
    @Path("/usuario/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserOrders(@PathParam("userId") Long userId) {
        try {
            List<OrderHeader> orders = service.getUserOrders(userId);
            return Response.ok(orders).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Estados válidos del flujo (para que el frontend muestre solo los siguientes).
     * GET /api/pedidos/flujo-estados
     */
    @GET
    @Path("/flujo-estados")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusFlow() {
        return Response.ok(service.getValidStatusFlow()).build();
    }

    /**
     * Obtener un pedido por ID
     * GET /api/pedidos/{orderId}
     */
    @GET
    @Path("/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderById(@PathParam("orderId") Long orderId) {
        try {
            OrderHeader order = service.getOrderById(orderId);
            if (order == null) {
                return Response.status(404).entity(new ErrorResponse(404, "Pedido no encontrado")).build();
            }

            // Construir respuesta completa con items y estado
            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("items", service.getOrderItems(orderId));
            response.put("status", service.getLatestStatus(orderId));

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Obtener historial de estados de un pedido
     * GET /api/pedidos/{orderId}/historial
     */
    @GET
    @Path("/{orderId}/historial")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderHistory(@PathParam("orderId") Long orderId) {
        try {
            List<OrderStatusHistory> history = service.getOrderHistory(orderId);
            return Response.ok(history).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    /**
     * Actualizar estado de un pedido (solo admin).
     * Body: status, comment?, changedByUserId, trackingNumber? (para SHIPPED), etaDays? (para SHIPPED).
     * PUT /api/pedidos/{orderId}/estado
     */
    @PUT
    @Path("/{orderId}/estado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrderStatus(@PathParam("orderId") Long orderId, Map<String, Object> body) {
        try {
            String status = (String) body.get("status");
            String comment = body.get("comment") != null ? body.get("comment").toString() : null;
            Long changedByUserId = body.get("changedByUserId") != null ?
                ((Number) body.get("changedByUserId")).longValue() : null;
            String trackingNumber = body.get("trackingNumber") != null ? body.get("trackingNumber").toString().trim() : null;
            Integer etaDays = null;
            if (body.get("etaDays") != null) {
                Object o = body.get("etaDays");
                etaDays = o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(o.toString());
            }

            if (status == null || status.isBlank()) {
                return Response.status(400).entity(new ErrorResponse(400, "status es obligatorio")).build();
            }

            service.updateOrderStatus(orderId, status, comment, changedByUserId, trackingNumber, etaDays);
            return Response.ok(Map.of("message", "Estado actualizado correctamente")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(new ErrorResponse(400, e.getMessage())).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage() != null ? e.getMessage() : "Error al actualizar estado")).build();
        }
    }

    /**
     * Listar todos los pedidos (admin/empleados) con filtros opcionales.
     * GET /api/pedidos?status=CONFIRMED&userId=1&from=2025-01-01&to=2025-12-31
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrders(
            @QueryParam("status") String status,
            @QueryParam("userId") Long userId,
            @QueryParam("from") String fromDate,
            @QueryParam("to") String toDate) {
        try {
            Date from = parseDate(fromDate, false);
            Date to = parseDate(toDate, true);
            List<OrderHeader> orders = (status != null || userId != null || from != null || to != null)
                ? service.getAllOrdersFiltered(status, userId, from, to)
                : service.getAllOrders();
            // Enriquecer con último estado para gestión admin
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (OrderHeader o : orders) {
                Map<String, Object> row = new HashMap<>();
                row.put("order", o);
                OrderStatusHistory latest = service.getLatestStatus(o.getOrderId());
                row.put("latestStatus", latest);
                result.add(row);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(500).entity(new ErrorResponse(500, e.getMessage())).build();
        }
    }

    private static Date parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;
        try {
            String time = endOfDay ? "T23:59:59.999Z" : "T00:00:00Z";
            return Date.from(ZonedDateTime.parse(value + time).toInstant());
        } catch (Exception e) {
            return null;
        }
    }
}
