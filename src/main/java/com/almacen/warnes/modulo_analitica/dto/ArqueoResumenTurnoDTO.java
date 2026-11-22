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
public class ArqueoResumenTurnoDTO {
    private Integer sesionId;
    private Integer sucursalId;
    private LocalDateTime horaInicioTurno;
    private Integer conteoVentas;
    private BigDecimal totalVentasEfectivo;
    private BigDecimal totalVentasQr;
    private BigDecimal totalVentasTarjeta;
    private BigDecimal totalVentasGeneral;
    private BigDecimal totalMovimientosEntrada;
    private BigDecimal totalMovimientosSalida;
    private Integer cantidadMovimientos;
    private Boolean requiereArqueo;
    private Object movimientosDetalle;
    private Object topProductos;
}
