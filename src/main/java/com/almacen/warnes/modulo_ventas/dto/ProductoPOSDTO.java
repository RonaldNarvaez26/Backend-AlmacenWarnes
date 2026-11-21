package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO de respuesta del buscador POS.
 * Proyección de fn_pos_buscar_producto(sucursal_id, criterio).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resultado del buscador POS: producto + stock del lote + indicador de descuento")
public class ProductoPOSDTO {

    @Schema(description = "ID del registro en Inventario (necesario para registrar la venta)", example = "5")
    @JsonProperty("inventarioId")
    private Integer inventarioId;

    @Schema(description = "ID del producto", example = "12")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Código de barras del producto", example = "7501234567890")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Código interno del almacén", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Nombre del producto", example = "Leche PIL 1L")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Precio oficial vigente al público", example = "10.50")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Stock disponible en este lote para esta sucursal (0 = Hard Stop)", example = "24")
    @JsonProperty("stockDisponible")
    private Integer stockDisponible;

    @Schema(description = "true si este lote está en liquidación/oferta", example = "false")
    @JsonProperty("enDescuento")
    private Boolean enDescuento;

    @Schema(description = "Precio especial del lote cuando en_descuento=true. Nullable.", example = "8.00")
    @JsonProperty("precioDescuento")
    private BigDecimal precioDescuento;
}

