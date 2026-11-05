package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para modificar el estado comercial de un producto en catálogo")
public class ActualizarEstadoProductoRequestDTO {

    @NotNull(message = "El estado es obligatorio (1 = Activo, 2 = Descontinuado, 3 = Suspendido)")
    @Min(value = 1, message = "El estado minimo valido es 1 (Activo)")
    @Max(value = 3, message = "El estado maximo valido es 3 (Suspendido)")
    @Schema(description = "Código de estado según Conceptos Grupo 3 (1: Activo, 2: Descontinuado, 3: Suspendido)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("estado")
    @JsonAlias({"nuevo_estado", "nuevoEstado"})
    private Integer estado;
}
