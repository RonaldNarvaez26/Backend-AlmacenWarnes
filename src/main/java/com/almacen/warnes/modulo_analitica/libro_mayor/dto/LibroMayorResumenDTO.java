package com.almacen.warnes.modulo_analitica.libro_mayor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibroMayorResumenDTO {
    private BigDecimal totalVentasQr;
    private BigDecimal totalVentasEfectivo;
    private BigDecimal totalVentasTarjeta;
    private BigDecimal totalVentas;
    private BigDecimal totalEntradasExtra;
    private BigDecimal totalSalidasExtra;
    private BigDecimal totalEntradas;
    private BigDecimal totalSalidas;
    private BigDecimal totalBajasMonto;
    private Integer cantidadBajas;
    private BigDecimal saldoNeto;
}
