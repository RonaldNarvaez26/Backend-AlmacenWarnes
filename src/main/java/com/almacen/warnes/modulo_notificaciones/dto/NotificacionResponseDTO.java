package com.almacen.warnes.modulo_notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponseDTO {
    private List<NotificacionItemDTO> notificaciones;
    private long totalCriticas;
    private long totalAltas;
    private long totalAdvertencias;
    private long totalInformativas;
    private long totalGeneral;
}
