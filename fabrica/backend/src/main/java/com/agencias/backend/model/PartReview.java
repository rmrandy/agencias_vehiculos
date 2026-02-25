package com.agencias.backend.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Comentario o valoración sobre un repuesto (Part).
 * Soporta comentarios multinivel: parentId null = comentario raíz (puede tener rating 1-5);
 * parentId no null = respuesta a otro comentario (sin rating).
 */
@Entity
@Table(name = "PART_REVIEW")
public class PartReview {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "part_review_seq")
    @SequenceGenerator(name = "part_review_seq", sequenceName = "PART_REVIEW_SEQ", allocationSize = 1)
    @Column(name = "REVIEW_ID")
    private Long reviewId;

    @Column(name = "PART_ID", nullable = false)
    private Long partId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "PARENT_ID")
    private Long parentId;

    /** Puntuación 1-5 estrellas. Solo en comentarios raíz (parentId null). */
    @Column(name = "RATING")
    private Integer rating;

    @Lob
    @Column(name = "BODY", nullable = false)
    private String body;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    public PartReview() {
    }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
