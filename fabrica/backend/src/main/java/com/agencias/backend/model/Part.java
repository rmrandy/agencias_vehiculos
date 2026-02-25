package com.agencias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "PART")
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "part_seq")
    @SequenceGenerator(name = "part_seq", sequenceName = "PART_SEQ", allocationSize = 1)
    @Column(name = "PART_ID")
    private Long partId;

    @Column(name = "CATEGORY_ID", nullable = false)
    private Long categoryId;

    @Column(name = "BRAND_ID", nullable = false)
    private Long brandId;

    @Column(name = "PART_NUMBER", nullable = false, unique = true, length = 100)
    private String partNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column
    private String description;

    @Column(name = "WEIGHT_LB", precision = 10, scale = 2)
    private BigDecimal weightLb;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer active = 1;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Lob
    @Column(name = "IMAGE_DATA")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "IMAGE_TYPE", length = 50)
    private String imageType;

    @Column(name = "STOCK_QUANTITY", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "LOW_STOCK_THRESHOLD", nullable = false)
    private Integer lowStockThreshold = 5;

    @Column(name = "RESERVED_QUANTITY", nullable = false)
    private Integer reservedQuantity = 0;

    @Transient
    private Boolean hasImage;

    @Transient
    private Boolean inStock;

    @Transient
    private Boolean lowStock;

    @Transient
    private Integer availableQuantity;

    @PostLoad
    public void postLoad() {
        this.hasImage = (imageData != null && imageData.length > 0);
        // Calcular disponibilidad
        this.availableQuantity = (stockQuantity != null ? stockQuantity : 0) - (reservedQuantity != null ? reservedQuantity : 0);
        this.inStock = this.availableQuantity > 0;
        this.lowStock = this.availableQuantity > 0 && this.availableQuantity <= (lowStockThreshold != null ? lowStockThreshold : 5);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (active == null) {
            active = 1;
        }
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (lowStockThreshold == null) {
            lowStockThreshold = 5;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
    }

    public Part() {
    }

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getWeightLb() { return weightLb; }
    public void setWeightLb(BigDecimal weightLb) { this.weightLb = weightLb; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
    public Boolean getHasImage() { return hasImage; }
    public void setHasImage(Boolean hasImage) { this.hasImage = hasImage; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    public Boolean getLowStock() { return lowStock; }
    public void setLowStock(Boolean lowStock) { this.lowStock = lowStock; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}
