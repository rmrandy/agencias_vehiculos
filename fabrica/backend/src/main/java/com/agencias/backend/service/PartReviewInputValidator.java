package com.agencias.backend.service;

/** Validación de comentarios/reseñas antes de consultar la BD. */
public final class PartReviewInputValidator {

    private PartReviewInputValidator() {
    }

    public static void validate(Long userId, Long parentId, Integer rating, String body) {
        if (userId == null) {
            throw new IllegalArgumentException("Debes iniciar sesión para comentar");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        boolean isRoot = parentId == null;
        if (isRoot && rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("La puntuación debe ser entre 1 y 5 estrellas");
        }
    }

    /** Las respuestas (no raíz) no llevan rating aunque el cliente envíe uno. */
    public static Integer normalizedRating(Long parentId, Integer rating) {
        boolean isRoot = parentId == null;
        if (!isRoot) {
            return null;
        }
        return rating;
    }
}
