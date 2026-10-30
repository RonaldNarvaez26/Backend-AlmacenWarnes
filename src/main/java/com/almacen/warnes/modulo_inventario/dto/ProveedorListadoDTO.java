package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorListadoDTO {

    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @JsonProperty("anotaciones")
    private String anotaciones;

    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("totalProductos")
    private Long totalProductos;

    @JsonProperty("totalLotes")
    private Long totalLotes;

    @JsonProperty("lotesActivos")
    private Long lotesActivos;

    @JsonProperty("lotesEnRiesgo")
    private Long lotesEnRiesgo;

    @JsonProperty("stockTotalAcumulado")
    private Long stockTotalAcumulado;

    @JsonProperty("valorMercaderia")
    private BigDecimal valorMercaderia;
}

