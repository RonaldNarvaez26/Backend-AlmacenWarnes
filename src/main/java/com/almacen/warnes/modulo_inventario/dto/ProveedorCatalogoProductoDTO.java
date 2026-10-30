package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Producto suministrado por un distribuidor específico")
public class ProveedorCatalogoProductoDTO {

    @Schema(description = "Identificador del producto", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Código interno del producto", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Código de barras", example = "777123456001")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Nombre o descripción comercial del producto", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Nombre de la categoría del producto", example = "Lácteos y Derivados")
    @JsonProperty("categoriaNombre")
    private String categoriaNombre;

    @Schema(description = "Precio de compra / costo de adquisición", example = "5.20")
    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @Schema(description = "Precio de venta al público", example = "6.00")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Estado del producto (1: Activo, 2: Descontinuado, 3: Suspendido)", example = "1")
    @JsonProperty("estadoProducto")
    private Integer estadoProducto;

    @Schema(description = "Stock físico actual en almacén", example = "78")
    @JsonProperty("stockActual")
    private Long stockActual;

    @Schema(description = "Total de lotes recibidos de este producto por este proveedor", example = "3")
    @JsonProperty("totalLotesRecibidos")
    private Long totalLotesRecibidos;

    @Schema(description = "Fecha de la última entrega registrada", example = "2026-10-29T13:28:59")
    @JsonProperty("fechaUltimaEntrega")
    private LocalDateTime fechaUltimaEntrega;
}
