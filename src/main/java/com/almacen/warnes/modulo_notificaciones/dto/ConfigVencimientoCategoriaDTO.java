package com.almacen.warnes.modulo_notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigVencimientoCategoriaDTO {
    private Integer categoriaId;
    private String nombreCategoria;
    private Integer diasAnticipacion;
    private Integer lotesAfectados;
}
