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
public class ReporteListadoDTO {
    private Integer reporteId;
    private Integer sucursalId;
    private String codigoSucursal;
    private String periodo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal montoBase;
    private BigDecimal montoEntradas;
    private BigDecimal montoSalidas;
    private BigDecimal utilidad;
    private Object datosSnapshot;
    private Integer usuarioCreacion;
    private String nombreUsuario;
    private LocalDateTime fechaCreacion;
}
