package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de generación de código interno correlativo según spGenerarCodigoInterno")
public class CodigoInternoDTO {

    @Schema(description = "Prefijo alfabético de 3 letras utilizado", example = "PRO")
    @JsonProperty("prefijo")
    private String prefijo;

    @Schema(description = "Código interno único generado (Prefijo + 3 dígitos)", example = "PRO012")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() {
        return codigoInterno;
    }
}
