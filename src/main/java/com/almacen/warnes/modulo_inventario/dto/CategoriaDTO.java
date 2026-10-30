package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaDTO {

    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @JsonProperty("codigo")
    private Integer codigo;

    @JsonProperty("nombreCategoria")
    private String nombreCategoria;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("categoria_id")
    public Integer getCategoria_id() {
        return categoriaId;
    }

    @JsonProperty("nombre_categoria")
    public String getNombre_categoria() {
        return nombreCategoria;
    }
}

