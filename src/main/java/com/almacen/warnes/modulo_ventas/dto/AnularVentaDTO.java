package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para anular una venta. Llama a sp_ventas_anular(p_venta_id, p_usuario_anulacion).
 * El SP valida que la venta exista y no esté ya anulada, y revierte el stock a los lotes originales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para anular una venta y revertir el stock")
public class AnularVentaDTO {

    @NotNull(message = "ventaId es obligatorio")
    @Schema(description = "ID de la venta a anular", example = "1234")
    @JsonProperty("ventaId")
    private Integer ventaId;

    @NotNull(message = "usuarioAnulacion es obligatorio")
    @Schema(description = "ID del usuario (Propietario o Supervisor) que autoriza la anulación", example = "2")
    @JsonProperty("usuarioAnulacion")
    private Integer usuarioAnulacion;
}

