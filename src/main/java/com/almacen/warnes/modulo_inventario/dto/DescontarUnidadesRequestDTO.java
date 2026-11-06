package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para descontar unidades específicas de stock en inventario (mermas, roturas, ajustes)")
public class DescontarUnidadesRequestDTO {

    @NotNull(message = "La cantidad a descontar es obligatoria")
    @Min(value = 1, message = "La cantidad a descontar debe ser como mínimo 1 unidad")
    @Schema(description = "Número de unidades a restar del stock disponible", example = "2", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    @JsonProperty("cantidad")
    private Integer cantidad;

    @Size(max = 100, message = "El motivo no puede superar los 100 caracteres")
    @Schema(description = "Motivo o justificación del retiro (rotura, vencimiento, merma, ajuste)", example = "Envase dañado en almacén", maxLength = 100)
    @JsonProperty("motivo")
    private String motivo;

    @Schema(description = "ID del usuario que realiza la operación", example = "2")
    @JsonProperty("usuarioId")
    private Integer usuarioId;
}

