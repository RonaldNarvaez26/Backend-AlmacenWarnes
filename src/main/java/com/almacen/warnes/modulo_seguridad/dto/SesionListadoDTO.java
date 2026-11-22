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
public class SesionListadoDTO {
    private Integer sesionId;
    private Integer usuarioId;
    private String usuario;
    private String nombreCompleto;
    private String descripcionRol;
    private Integer sucursalId;
    private String codigoSucursal;
    private LocalDateTime horaSesionAbierta;
    private LocalDateTime horaSesionCerrada;
    private Integer duracionSesion;
    private Boolean estaActiva;
    private Integer reporteId;
    private String estadoArqueo;
    private Boolean alertaSinArqueo;
}
