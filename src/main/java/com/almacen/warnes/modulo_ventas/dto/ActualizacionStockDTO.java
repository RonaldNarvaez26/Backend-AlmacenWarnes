package com.almacen.warnes.modulo_ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload emitido por WebSocket hacia /topic/sucursal/{sucursalId}/stock
 * tras registrar o anular una venta para sincronización en tiempo real.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evento de actualización de stock en tiempo real vía WebSocket")
public class ActualizacionStockDTO {

    @Schema(description = "Tipo de evento que originó el cambio", example = "VENTA_REGISTRADA")
    private String tipoEvento; // VENTA_REGISTRADA, VENTA_ANULADA

    @Schema(description = "ID de la sucursal afectada", example = "1")
    private Integer sucursalId;

    @Schema(description = "ID de la venta relacionada", example = "105")
    private Integer ventaId;

    @Schema(description = "Lista de lotes de inventario con su nuevo stock disponible")
    private List<ItemStockActualizadoDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemStockActualizadoDTO {
        @Schema(description = "ID de la fila en tabla Inventario", example = "12")
        private Integer inventarioId;

        @Schema(description = "ID del producto", example = "4")
        private Integer productoId;

        @Schema(description = "Stock físico restante y disponible en este momento", example = "8")
        private Integer nuevoStock;
    }
}
