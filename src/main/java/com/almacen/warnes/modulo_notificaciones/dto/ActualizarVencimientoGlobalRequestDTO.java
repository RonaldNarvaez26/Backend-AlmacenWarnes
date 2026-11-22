package com.almacen.warnes.modulo_notificaciones.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarVencimientoGlobalRequestDTO {
    @NotNull(message = "Los días de anticipación son obligatorios")
    @Min(value = 1, message = "Los días de anticipación deben ser al menos 1")
    @Max(value = 365, message = "Los días de anticipación no pueden superar los 365 días")
    private Integer diasAnticipacion;
}
