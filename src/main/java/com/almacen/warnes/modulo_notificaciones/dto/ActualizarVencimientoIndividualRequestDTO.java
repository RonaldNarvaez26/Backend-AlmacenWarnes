package com.almacen.warnes.modulo_notificaciones.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarVencimientoIndividualRequestDTO {
    private Integer categoriaId;

    @Min(value = 1, message = "Los días de anticipación deben ser al menos 1")
    @Max(value = 365, message = "Los días de anticipación no pueden superar los 365 días")
    private Integer diasAnticipacion;

    @Valid
    private List<ItemCategoriaDiasDTO> categorias;
}
