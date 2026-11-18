package com.almacen.warnes.modulo_notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionStockVentaDTO {
    private Boolean puedeVender;
    private Integer stockActual;
    private BigDecimal stockRestante;
    private String nivelAlerta;
    private String mensajeAlerta;
}
