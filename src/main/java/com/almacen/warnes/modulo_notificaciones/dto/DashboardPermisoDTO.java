package com.almacen.warnes.modulo_notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPermisoDTO {
    private Integer supervisorId;
    private Boolean tieneAcceso;
    private String estado;
    private String mensaje;
}
