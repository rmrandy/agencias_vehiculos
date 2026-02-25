package com.agencias.backend.controller.dto;

import java.util.Date;
import java.util.List;

/**
 * DTO para un comentario en el árbol (incluye nombre de usuario y respuestas anidadas).
 */
public class PartReviewDto {
    private Long reviewId;
    private Long partId;
    private Long userId;
    private Long parentId;
    private Integer rating;
    private String body;
    private Date createdAt;
    private String userDisplayName;
    private List<PartReviewDto> children;

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
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public List<PartReviewDto> getChildren() { return children; }
    public void setChildren(List<PartReviewDto> children) { this.children = children; }
}
