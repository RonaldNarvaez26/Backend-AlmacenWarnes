package com.almacen.warnes.modulo_conceptos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para modificar la descripción de un concepto existente")
public class ModificarConceptoDTO {

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 80, message = "La descripción no puede superar los 80 caracteres")
    @Schema(description = "Nueva descripción para el concepto", example = "Alimentos y Víveres", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("descripcion")
    private String descripcion;

    @Schema(description = "ID del usuario que realiza la modificación (opcional si se envía token)", example = "1")
    @JsonProperty("usuarioModificacion")
    private Integer usuarioModificacion;

    @JsonProperty("usuario_modificacion")
    public void setUsuario_modificacion(Integer u) {
        if (this.usuarioModificacion == null) {
            this.usuarioModificacion = u;
        }
    }
}

