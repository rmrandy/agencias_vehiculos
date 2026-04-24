package com.agencias.backend.controller.dto;

import java.util.List;

public class ComentariosResponse {
    private Double promedio;
    private List<PartReviewDto> comentarios;

    public Double getPromedio() { return promedio; }
    public void setPromedio(Double promedio) { this.promedio = promedio; }
    public List<PartReviewDto> getComentarios() { return comentarios; }
    public void setComentarios(List<PartReviewDto> comentarios) { this.comentarios = comentarios; }
}
