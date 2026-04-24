package com.agencias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "BRAND")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brand_seq")
    @SequenceGenerator(name = "brand_seq", sequenceName = "BRAND_SEQ", allocationSize = 1)
    @Column(name = "BRAND_ID")
    private Long brandId;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    @Column(name = "IMAGE_DATA")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "IMAGE_TYPE", length = 50)
    private String imageType;

    public Brand() {
    }

    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
}
