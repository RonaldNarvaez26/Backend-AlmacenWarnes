package com.almacen.warnes.modulo_analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteDetalleDTO {
    private Integer reporteId;
    private Integer sucursalId;
    private String codigoSucursal;
    private String nombreSucursal;
    private String periodo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal montoBase;
    private BigDecimal montoEntradas;
    private BigDecimal montoSalidas;
    private BigDecimal utilidad;
    private Object datosSnapshot;
    private String nombreUsuario;
    private LocalDateTime fechaCreacion;
    private Object entradas;
    private Object salidas;
    private Object movimientos;
}
