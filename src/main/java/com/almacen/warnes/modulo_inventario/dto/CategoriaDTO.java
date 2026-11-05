package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Categoría de producto paramétrica (Conceptos Grupo 2)")
public class CategoriaDTO {

    @Schema(description = "ID del concepto en base de datos", example = "6")
    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @Schema(description = "Código numérico de la categoría dentro del grupo 2", example = "1")
    @JsonProperty("codigo")
    private Integer codigo;

    @Schema(description = "Nombre formal de la categoría", example = "Alimentos")
    @JsonProperty("nombreCategoria")
    private String nombreCategoria;

    @Schema(description = "Descripción de la categoría", example = "Alimentos de consumo directo y procesados")
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
