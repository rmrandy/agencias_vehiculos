package com.agencias.backend.service;

import com.agencias.backend.controller.dto.PartReviewDto;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.PartReview;
import com.agencias.backend.repository.PartReviewRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PartReviewService {
    private final PartReviewRepository reviewRepo;
    private final UserService userService;

    public PartReviewService(EntityManagerFactory emf) {
        this.reviewRepo = new PartReviewRepository(emf);
        this.userService = new UserService(emf);
    }

    /**
     * Crea un comentario. Solo usuarios registrados (userId debe existir).
     * En comentarios raíz (parentId null) se puede enviar rating 1-5.
     */
    public PartReview create(Long partId, Long userId, Long parentId, Integer rating, String body) {
        if (userId == null) {
            throw new IllegalArgumentException("Debes iniciar sesión para comentar");
        }
        AppUser user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        boolean isRoot = (parentId == null);
        if (isRoot && rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("La puntuación debe ser entre 1 y 5 estrellas");
        }
        if (!isRoot && rating != null) {
            rating = null; // las respuestas no llevan rating
        }

        PartReview r = new PartReview();
        r.setPartId(partId);
        r.setUserId(userId);
        r.setParentId(parentId);
        r.setRating(isRoot ? rating : null);
        r.setBody(body.trim());
        return reviewRepo.save(r);
    }

    /**
     * Devuelve los comentarios del producto en árbol (solo raíces, cada uno con children).
     */
    public List<PartReviewDto> getTreeByPartId(Long partId) {
        List<PartReview> all = reviewRepo.findByPartId(partId);
        Map<Long, AppUser> users = loadUserDisplayNames(all);
        List<PartReview> roots = all.stream().filter(r -> r.getParentId() == null).toList();
        List<PartReviewDto> result = new ArrayList<>();
        for (PartReview root : roots) {
            result.add(toDto(root, all, users));
        }
        return result;
    }

    public Double getAverageRating(Long partId) {
        return reviewRepo.averageRatingByPartId(partId);
    }

    private Map<Long, AppUser> loadUserDisplayNames(List<PartReview> reviews) {
        List<Long> userIds = reviews.stream().map(PartReview::getUserId).distinct().toList();
        Map<Long, AppUser> map = new java.util.HashMap<>();
        for (Long uid : userIds) {
            AppUser u = userService.getById(uid);
            if (u != null) map.put(uid, u);
        }
        return map;
    }

    private PartReviewDto toDto(PartReview r, List<PartReview> all, Map<Long, AppUser> users) {
        PartReviewDto dto = new PartReviewDto();
        dto.setReviewId(r.getReviewId());
        dto.setPartId(r.getPartId());
        dto.setUserId(r.getUserId());
        dto.setParentId(r.getParentId());
        dto.setRating(r.getRating());
        dto.setBody(r.getBody());
        dto.setCreatedAt(r.getCreatedAt());
        AppUser u = users.get(r.getUserId());
        dto.setUserDisplayName(u != null ? (u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail()) : "Usuario");
        List<PartReview> children = all.stream().filter(c -> r.getReviewId().equals(c.getParentId())).toList();
        dto.setChildren(children.stream().map(c -> toDto(c, all, users)).collect(Collectors.toList()));
        return dto;
    }
}
