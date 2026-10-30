package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorHistorialLoteDTO {

    @JsonProperty("loteId")
    private Integer loteId;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("cantidadInicial")
    private Integer cantidadInicial;

    @JsonProperty("stockActual")
    private Integer stockActual;

    @JsonProperty("costoUnitario")
    private BigDecimal costoUnitario;

    @JsonProperty("precioLote")
    private BigDecimal precioLote;

    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @JsonProperty("diasParaVencer")
    private Integer diasParaVencer;

    @JsonProperty("estadoCaducidad")
    private String estadoCaducidad;

    @JsonProperty("fechaIngreso")
    private LocalDateTime fechaIngreso;
}

