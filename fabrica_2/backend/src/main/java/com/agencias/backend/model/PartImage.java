package com.agencias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Imagen adicional de un repuesto (galería 2–5 fotos en total con {@link Part#imageData}).
 */
@Entity
@Table(name = "PART_IMAGE")
public class PartImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "part_image_seq")
    @SequenceGenerator(name = "part_image_seq", sequenceName = "part_image_seq", allocationSize = 1)
    @Column(name = "IMAGE_ID")
    private Long imageId;

    @Column(name = "PART_ID", nullable = false)
    private Long partId;

    @Column(name = "URL_PATH", length = 500)
    private String urlPath;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder = 0;

    @Lob
    @Column(name = "IMAGE_DATA")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "IMAGE_TYPE", length = 50)
    private String imageType;

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }
}
