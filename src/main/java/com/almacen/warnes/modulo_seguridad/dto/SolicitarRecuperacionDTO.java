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
public class SolicitarRecuperacionDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String usuario;
}

