package com.almacen.warnes.modulo_analitica.libro_mayor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularMovimientoRequestDTO {
    private String origen; // 'VENTA' o 'EXTRA_VENTA'
    private String motivo;
}
