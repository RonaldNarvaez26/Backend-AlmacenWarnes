package com.almacen.warnes.modulo_seguridad.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioModificarDTO {

    private Integer sucursalId;

    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    private Integer rolId;

    @NotNull(message = "El usuario modificador es obligatorio")
    private Integer usuarioModificacion;
}

