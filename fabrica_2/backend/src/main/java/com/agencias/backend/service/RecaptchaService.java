package com.agencias.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RecaptchaService {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double MIN_SCORE = 0.5; // Score mínimo para reCAPTCHA v3
    private final String secretKey;
    private final ObjectMapper objectMapper;

    public RecaptchaService(String secretKey) {
        this.secretKey = secretKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Valida el token de reCAPTCHA v3 con Google
     * @param recaptchaResponse El token del frontend
     * @return true si es válido y el score es >= 0.5, false si no
     */
    public boolean verify(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }

        try {
            // Construir parámetros
            String params = "secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8) +
                          "&response=" + URLEncoder.encode(recaptchaResponse, StandardCharsets.UTF_8);

            // Hacer request a Google
            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            // Enviar parámetros
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            // Leer respuesta
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    // Parsear JSON
                    JsonNode jsonNode = objectMapper.readTree(response.toString());
                    boolean success = jsonNode.has("success") && jsonNode.get("success").asBoolean();
                    
                    // reCAPTCHA v3 incluye un score (0.0 a 1.0)
                    // 0.0 = muy probablemente un bot
                    // 1.0 = muy probablemente un humano
                    if (success && jsonNode.has("score")) {
                        double score = jsonNode.get("score").asDouble();
                        System.out.println("reCAPTCHA score: " + score);
                        return score >= MIN_SCORE;
                    }
                    
                    return success;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error al verificar reCAPTCHA: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida el token y lanza excepción si es inválido o el score es bajo
     */
    public void verifyOrThrow(String recaptchaResponse) {
        if (!verify(recaptchaResponse)) {
            throw new IllegalArgumentException("Verificación de reCAPTCHA fallida. Tu actividad parece sospechosa. Por favor, intenta de nuevo.");
        }
    }
}
