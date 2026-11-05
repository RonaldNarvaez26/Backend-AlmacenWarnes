package com.almacen.warnes.modulo_seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecuperarContrasenaDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String usuario;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String nuevaPassword;

    private Integer usuarioModificacion;
}

