package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para registrar una nueva categoría de producto en la tabla conceptos (Grupo 2)")
public class CrearCategoriaRequestDTO {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre de la categoría no puede superar los 80 caracteres")
    @Schema(description = "Nombre o descripción formal de la nueva categoría", example = "Limpieza", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    @JsonProperty("nombre")
    private String nombre;

    @Schema(description = "ID del usuario que registra la categoría (por defecto 1)", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @JsonProperty("descripcion")
    public void setDescripcion(String descripcion) {
        if (this.nombre == null || this.nombre.trim().isEmpty()) {
            this.nombre = descripcion;
        }
    }
}

