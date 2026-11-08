package com.almacen.warnes.modulo_conceptos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Representación de un concepto paramétrico de la tabla Conceptos")
public class ConceptoDTO {

    @Schema(description = "Identificador único de la tupla concepto", example = "6")
    @JsonProperty("conceptoId")
    private Integer conceptoId;

    @Schema(description = "Identificador del grupo al que pertenece", example = "2")
    @JsonProperty("grupoId")
    private Integer grupoId;

    @Schema(description = "Código secuencial dentro del grupo (excluyendo el código 0)", example = "1")
    @JsonProperty("codigo")
    private Integer codigo;

    @Schema(description = "Descripción o nombre del concepto", example = "Alimentos")
    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("concepto_id")
    public Integer getConcepto_id() {
        return conceptoId;
    }

    @JsonProperty("grupo_id")
    public Integer getGrupo_id() {
        return grupoId;
    }
}

