package com.almacen.warnes.modulo_conceptos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para registrar un nuevo concepto paramétrico")
public class CrearConceptoDTO {

    @NotNull(message = "El grupoId es obligatorio")
    @Schema(description = "ID del grupo (ej: 2 para Categorías de producto)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("grupoId")
    private Integer grupoId;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 80, message = "La descripción no puede superar los 80 caracteres")
    @Schema(description = "Nombre o descripción formal del nuevo concepto", example = "Ferretería", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("descripcion")
    private String descripcion;

    @Schema(description = "ID del usuario que realiza la creación (opcional si se envía token)", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @JsonProperty("grupo_id")
    public void setGrupo_id(Integer g) {
        if (this.grupoId == null) {
            this.grupoId = g;
        }
    }

    @JsonProperty("usuario_creacion")
    public void setUsuario_creacion(Integer u) {
        if (this.usuarioCreacion == null) {
            this.usuarioCreacion = u;
        }
    }
}

