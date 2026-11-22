package com.almacen.warnes.modulo_seguridad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionDTO {
    private Integer sesionId;
    private Integer usuarioId;
    private LocalDateTime horaSesionAbierta;
    private Integer minutosTranscurridos;
}
