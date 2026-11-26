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
public class MovimientoListadoDTO {
    private Integer movimientoId;
    private Integer reporteId;
    private BigDecimal monto;
    private Integer tipoMovimiento;
    private String descripcionTipo;
    private Integer metodoMovimiento;
    private String descripcionMetodo;
    private String detalle;
    private Integer usuarioCreacion;
    private String nombreUsuario;
    private Integer sucursalId;
    private LocalDateTime fechaCreacion;
    private Boolean sellado;
    private Integer rolUsuario;
    private Boolean requiereConsolidacion;
}
