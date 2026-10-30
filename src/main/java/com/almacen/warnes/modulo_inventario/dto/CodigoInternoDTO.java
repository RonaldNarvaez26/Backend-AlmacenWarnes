package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoInternoDTO {

    @JsonProperty("prefijo")
    private String prefijo;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() {
        return codigoInterno;
    }
}

