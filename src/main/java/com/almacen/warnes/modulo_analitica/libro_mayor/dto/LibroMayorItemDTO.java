package com.almacen.warnes.modulo_analitica.libro_mayor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibroMayorItemDTO {
    private String referenciaId;
    private LocalDateTime fechaHora;
    private String origen; // 'VENTA' o 'EXTRA_VENTA'
    private Integer tipoMovimiento; // 1 = Entrada, 2 = Salida
    private String tipo; // 'ENTRADA' o 'SALIDA'
    private Integer metodoMovimiento; // 1 = Efectivo, 2 = QR, etc.
    private String metodo; // 'EFECTIVO', 'QR', etc.
    private Integer categoriaId;
    private String categoria;
    private String detalle;
    private BigDecimal monto;
    private Boolean marcaBaja;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer cajeroId;
    private String cajeroNombre;
    private String productosResumen;
}
