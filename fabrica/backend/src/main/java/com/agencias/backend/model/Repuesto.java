package com.agencias.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "REPUESTOS")
public class Repuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "repuesto_seq")
    @SequenceGenerator(name = "repuesto_seq", sequenceName = "REPUESTO_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    public Repuesto() {
    }

    public Repuesto(String nombre, BigDecimal precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
