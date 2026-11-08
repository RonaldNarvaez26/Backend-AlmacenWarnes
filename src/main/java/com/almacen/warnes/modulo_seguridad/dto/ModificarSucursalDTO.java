package com.almacen.warnes.modulo_seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para modificar una sucursal existente")
public class ModificarSucursalDTO {

    @Size(max = 50, message = "El código interno no puede superar los 50 caracteres")
    @Schema(description = "Código interno de la sucursal", example = "SUC-002")
    private String codigoInterno;

    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    @Schema(description = "Ciudad donde se ubica la sucursal", example = "Montero")
    private String ciudad;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Schema(description = "Dirección física de la sucursal", example = "Calle Comercio #125, Zona Norte")
    private String direccion;

    @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
    @Schema(description = "Teléfono de contacto", example = "39225577")
    private String telefono;
}

