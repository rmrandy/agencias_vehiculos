package com.agencias.backend.service;

import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.Part;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import jakarta.mail.*;
import jakarta.mail.internet.*;

/**
 * Servicio de envío de correo. Si no hay SMTP configurado, simula el envío (log).
 */
public class MailService {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final boolean enabled;

    public MailService() {
        Properties mailProps = loadMailProperties();
        this.host = firstNonBlank(
            System.getenv("MAIL_HOST"),
            mailProps.getProperty("mail.smtp.host")
        );
        this.port = parsePort(
            firstNonBlank(System.getenv("MAIL_PORT"), mailProps.getProperty("mail.smtp.port"))
        );
        this.user = firstNonBlank(
            System.getenv("MAIL_USER"),
            mailProps.getProperty("mail.sender.email")
        );
        this.password = firstNonBlank(
            System.getenv("MAIL_PASSWORD"),
            mailProps.getProperty("mail.sender.password")
        );
        this.enabled = host != null && !host.isBlank() && user != null && !user.isBlank() && password != null && !password.isBlank();
        if (!enabled && (host != null || user != null)) {
            System.out.println("[MailService] Correo no enviado: configura mail.sender.email y mail.sender.password en mail.properties (o MAIL_USER y MAIL_PASSWORD) para enviar correos reales.");
        }
    }

    private static Properties loadMailProperties() {
        Properties p = new Properties();
        try (InputStream in = MailService.class.getResourceAsStream("/mail.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (Exception e) {
            // Ignorar: usaremos env o simulación
        }
        return p;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private static int parsePort(String value) {
        if (value == null || value.isBlank()) return 587;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 587;
        }
    }

    /**
     * Envía un correo con el detalle del pedido al comprador.
     * Si SMTP no está configurado, escribe el contenido en log (simulación).
     */
    public void sendOrderConfirmation(String toEmail, String customerName, OrderHeader order,
                                       List<OrderItem> items, Function<Long, Optional<Part>> partById) {
        String subject = "Confirmación de pedido #" + order.getOrderNumber();
        String htmlBody = buildOrderEmailHtml(order, items, customerName, partById);

        if (enabled) {
            try {
                sendMail(toEmail, subject, htmlBody);
            } catch (Exception e) {
                System.err.println("[MailService] Error enviando correo real: " + e.getMessage());
                logSimulatedEmail(toEmail, subject, htmlBody);
            }
        } else {
            logSimulatedEmail(toEmail, subject, htmlBody);
        }
    }

    private void logSimulatedEmail(String to, String subject, String htmlBody) {
        System.out.println("---------- CORREO SIMULADO ----------");
        System.out.println("Para: " + to);
        System.out.println("Asunto: " + subject);
        System.out.println("Contenido (HTML):");
        System.out.println(htmlBody);
        System.out.println("------------------------------------");
    }

    private void sendMail(String to, String subject, String htmlContent) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password != null ? password : "");
            }
        });

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(user, "Fábrica - Agencias Vehículos", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(user));
        }
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject, "UTF-8");
        msg.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(msg);
    }

    private String buildOrderEmailHtml(OrderHeader order, List<OrderItem> items, String customerName,
                                       Function<Long, Optional<Part>> partById) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : items) {
            String title = partById.apply(item.getPartId()).map(Part::getTitle).orElse("Repuesto");
            rows.append("<tr><td>").append(escape(title))
                .append("</td><td>").append(item.getQty())
                .append("</td><td>").append(item.getUnitPrice())
                .append("</td><td>").append(item.getLineTotal())
                .append("</td></tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: sans-serif;'>"
            + "<h2>Gracias por tu compra</h2>"
            + "<p>Hola" + (customerName != null && !customerName.isBlank() ? " " + escape(customerName) : "") + ",</p>"
            + "<p>Tu pedido ha sido registrado correctamente.</p>"
            + "<p><strong>Número de pedido:</strong> " + escape(order.getOrderNumber()) + "</p>"
            + "<table border='1' cellpadding='8' style='border-collapse: collapse;'>"
            + "<thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unit.</th><th>Total</th></tr></thead>"
            + "<tbody>" + rows + "</tbody>"
            + "</table>"
            + "<p><strong>Subtotal:</strong> " + order.getSubtotal() + " " + order.getCurrency() + "</p>"
            + "<p><strong>Envío:</strong> " + order.getShippingTotal() + " " + order.getCurrency() + "</p>"
            + "<p><strong>Total:</strong> " + order.getTotal() + " " + order.getCurrency() + "</p>"
            + "<p>Puedes ver el detalle y seguimiento en <strong>Mis Pedidos</strong> en la aplicación.</p>"
            + "<p>— Fábrica Agencias Vehículos</p>"
            + "</body></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Envía correo al cliente cuando se actualiza el estado del pedido (comentario, tracking y ETA si aplican).
     */
    public void sendOrderStatusUpdate(String toEmail, String customerName, String orderNumber, String newStatus,
                                       String comment, String trackingNumber, Integer etaDays) {
        String statusLabel = formatStatusLabel(newStatus);
        String subject = "Actualización de pedido #" + orderNumber;
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: sans-serif;'>");
        body.append("<h2>Actualización de tu pedido</h2>");
        body.append("<p>Hola").append(customerName != null && !customerName.isBlank() ? " " + escape(customerName) : "").append(",</p>");
        body.append("<p>El estado de tu pedido <strong>").append(escape(orderNumber)).append("</strong> ha cambiado:</p>");
        body.append("<p><strong>Nuevo estado:</strong> ").append(escape(statusLabel)).append("</p>");
        if (comment != null && !comment.isBlank()) {
            body.append("<p><strong>Comentario:</strong> ").append(escape(comment)).append("</p>");
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            body.append("<p><strong>Número de seguimiento:</strong> ").append(escape(trackingNumber)).append("</p>");
        }
        if (etaDays != null && etaDays > 0) {
            body.append("<p><strong>Tiempo estimado de entrega:</strong> ").append(etaDays).append(" días</p>");
        }
        body.append("<p>Puedes ver el detalle en <strong>Mis Pedidos</strong> en la aplicación.</p>");
        body.append("<p>— Fábrica Agencias Vehículos</p></body></html>");

        String htmlBody = body.toString();
        if (enabled) {
            try {
                sendMail(toEmail, subject, htmlBody);
            } catch (Exception e) {
                System.err.println("[MailService] Error enviando correo de actualización: " + e.getMessage());
            }
        } else {
            logSimulatedEmail(toEmail, subject, htmlBody);
        }
    }

    private static String formatStatusLabel(String status) {
        if (status == null) return status;
        switch (status.toUpperCase()) {
            case "INITIATED": return "Iniciada";
            case "CONFIRMED": return "Confirmado";
            case "IN_PREPARATION": case "PREPARING": return "Preparación del pedido";
            case "SHIPPED": return "Enviado";
            case "DELIVERED": return "Entregado";
            case "CANCELLED": return "Cancelado";
            default: return status;
        }
    }

    /**
     * Envía alerta de bajo stock a los correos configurados en mail.admin.emails.
     */
    public void sendLowStockAlert(Part part) {
        List<String> toList = getAdminEmails();
        if (toList.isEmpty()) return;

        int available = (part.getStockQuantity() != null ? part.getStockQuantity() : 0)
            - (part.getReservedQuantity() != null ? part.getReservedQuantity() : 0);
        String subject = "Alerta: bajo stock - " + part.getTitle();
        String htmlBody = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: sans-serif;'>"
            + "<h2>Alerta de bajo stock</h2>"
            + "<p>El siguiente producto ha llegado al umbral de bajo inventario:</p>"
            + "<ul>"
            + "<li><strong>Producto:</strong> " + escape(part.getTitle()) + "</li>"
            + "<li><strong>No. parte:</strong> " + escape(part.getPartNumber()) + "</li>"
            + "<li><strong>Disponible:</strong> " + available + "</li>"
            + "<li><strong>Umbral:</strong> " + (part.getLowStockThreshold() != null ? part.getLowStockThreshold() : 5) + "</li>"
            + "</ul>"
            + "<p>— Fábrica Agencias Vehículos</p></body></html>";

        for (String to : toList) {
            if (enabled) {
                try {
                    sendMail(to.trim(), subject, htmlBody);
                } catch (Exception e) {
                    System.err.println("[MailService] Error enviando alerta bajo stock a " + to + ": " + e.getMessage());
                }
            } else {
                logSimulatedEmail(to.trim(), subject, htmlBody);
            }
        }
    }

    private List<String> getAdminEmails() {
        Properties p = loadMailProperties();
        String env = System.getenv("MAIL_ADMIN_EMAILS");
        String prop = p.getProperty("mail.admin.emails");
        String value = (env != null && !env.isBlank()) ? env : (prop != null ? prop : "");
        if (value == null || value.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.asList(value.split(",\\s*"));
    }
}
