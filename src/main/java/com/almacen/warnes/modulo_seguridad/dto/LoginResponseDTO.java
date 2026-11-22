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
public class LoginResponseDTO {
    private String token;
    private Integer usuarioId;
    private Integer sucursalId;
    private String usuario;
    private String nombres;
    private String apellidos;
    private Integer rolId;
    private Integer estado;
    private Integer sesionId;
    private LocalDateTime horaSesionAbierta;
    private String mensaje;
}

