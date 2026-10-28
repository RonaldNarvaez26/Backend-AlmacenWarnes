package com.almacen.warnes.modulo_seguridad.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Integer usuarioId;
    private String usuario;
    private String nombres;
    private String apellidos;
    private Integer rolId;
    private Boolean marcaBaja;
    private LocalDateTime fechaCreacion;
    private Integer usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private Integer usuarioModificacion;
}
