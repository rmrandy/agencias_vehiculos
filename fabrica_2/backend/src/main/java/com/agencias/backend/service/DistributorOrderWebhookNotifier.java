package com.agencias.backend.service;

import com.agencias.backend.config.ConfigLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Notifica a la distribuidora un cambio de estado del pedido en fábrica (pedidos con origen DISTRIBUIDORA).
 * URL y secreto: variables de entorno {@code DISTRIBUIDORA_PEDIDOS_WEBHOOK_URL} y
 * {@code DISTRIBUIDORA_PEDIDOS_WEBHOOK_SECRET}, o {@code distribuidora.pedidos.webhook.url} /
 * {@code distribuidora.pedidos.webhook.secret} en application.properties.
 */
public final class DistributorOrderWebhookNotifier {

    private DistributorOrderWebhookNotifier() {
    }

    public static void notifyOrderStatusAsync(
        long fabricaOrderId,
        String status,
        String comment,
        String trackingNumber,
        Integer etaDays
    ) {
        String url = firstNonBlank(
            System.getenv("DISTRIBUIDORA_PEDIDOS_WEBHOOK_URL"),
            prop("distribuidora.pedidos.webhook.url")
        );
        if (url == null || url.isBlank()) {
            return;
        }
        String secret = firstNonBlank(
            System.getenv("DISTRIBUIDORA_PEDIDOS_WEBHOOK_SECRET"),
            prop("distribuidora.pedidos.webhook.secret")
        );
        if (secret == null || secret.isBlank()) {
            System.err.println("DistributorOrderWebhookNotifier: falta secreto (DISTRIBUIDORA_PEDIDOS_WEBHOOK_SECRET o distribuidora.pedidos.webhook.secret)");
            return;
        }

        String json = buildJson(fabricaOrderId, status, comment, trackingNumber, etaDays);
        CompletableFuture.runAsync(() -> postJson(url.trim(), secret.trim(), json));
    }

    private static String prop(String key) {
        try {
            Properties p = ConfigLoader.loadProperties();
            String v = p.getProperty(key);
            return v != null ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String buildJson(long fabricaOrderId, String status, String comment, String trackingNumber, Integer etaDays) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"fabricaOrderId\":").append(fabricaOrderId);
        sb.append(",\"status\":\"").append(jsonEscape(status != null ? status : "")).append('"');
        if (comment != null && !comment.isBlank()) {
            sb.append(",\"comment\":\"").append(jsonEscape(comment)).append('"');
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            sb.append(",\"trackingNumber\":\"").append(jsonEscape(trackingNumber)).append('"');
        }
        if (etaDays != null) {
            sb.append(",\"etaDays\":").append(etaDays);
        }
        sb.append('}');
        return sb.toString();
    }

    private static void postJson(String url, String secret, String json) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Fabrica-Webhook-Secret", secret)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.println("DistributorOrderWebhookNotifier: HTTP " + resp.statusCode() + " body=" + resp.body());
            }
        } catch (Exception e) {
            System.err.println("DistributorOrderWebhookNotifier: " + e.getMessage());
        }
    }
}
