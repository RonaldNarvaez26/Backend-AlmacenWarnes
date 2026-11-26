package com.almacen.warnes.modulo_seguridad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambiarEstadoUsuarioDTO {

    @NotNull(message = "El estado es obligatorio (1=Activo, 3=Bloqueado)")
    private Integer estado;

    private Integer usuarioModificacion;
}
