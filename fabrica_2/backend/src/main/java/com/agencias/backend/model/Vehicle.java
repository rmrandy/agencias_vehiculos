package com.agencias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "VEHICLE")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicle_seq")
    @SequenceGenerator(name = "vehicle_seq", sequenceName = "VEHICLE_SEQ", allocationSize = 1)
    @Column(name = "VEHICLE_ID")
    private Long vehicleId;

    @Column(name = "UNIVERSAL_VEHICLE_CODE", nullable = false, length = 100)
    private String universalVehicleCode;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String line;

    @Column(name = "YEAR_NUMBER")
    private Integer yearNumber;

    @Lob
    @Column(name = "IMAGE_DATA")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "IMAGE_TYPE", length = 50)
    private String imageType;

    public Vehicle() {
    }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getUniversalVehicleCode() { return universalVehicleCode; }
    public void setUniversalVehicleCode(String universalVehicleCode) { this.universalVehicleCode = universalVehicleCode; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
    public Integer getYearNumber() { return yearNumber; }
    public void setYearNumber(Integer yearNumber) { this.yearNumber = yearNumber; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
}
