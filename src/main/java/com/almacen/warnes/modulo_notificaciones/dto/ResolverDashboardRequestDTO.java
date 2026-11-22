package com.almacen.warnes.modulo_notificaciones.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolverDashboardRequestDTO {

    @NotNull(message = "El ID del supervisor es obligatorio")
    private Integer supervisorId;

    @NotNull(message = "Debe especificar si autoriza (true) o deniega (false)")
    private Boolean autorizar;
}
