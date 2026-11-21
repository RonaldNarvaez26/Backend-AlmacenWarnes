package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para un ítem del carrito enviado al backend al registrar la venta.
 * Soporta tanto camelCase como snake_case para máxima interoperabilidad.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Un ítem del carrito de venta")
public class DetalleVentaDTO {

    @Schema(description = "ID del detalle de venta (generado en BD)", example = "10")
    @JsonProperty("detalleVentaId")
    @JsonAlias({"detalleVentaId", "detalle_venta_id"})
    private Integer detalleVentaId;

    @Schema(description = "ID del registro en Inventario del que se descontará el stock", example = "5")
    @JsonProperty("inventarioId")
    @JsonAlias({"inventarioId", "inventario_id"})
    private Integer inventarioId;

    @Schema(description = "ID del producto vendido", example = "12")
    @JsonProperty("productoId")
    @JsonAlias({"productoId", "producto_id"})
    private Integer productoId;

    @Schema(description = "Nombre del producto", example = "Leche Entera PIL 946ml")
    @JsonProperty("nombreProducto")
    @JsonAlias({"nombreProducto", "nombre_producto"})
    private String nombreProducto;

    @Schema(description = "Código interno del producto", example = "LEC001")
    @JsonProperty("codigoInterno")
    @JsonAlias({"codigoInterno", "codigo_interno"})
    private String codigoInterno;

    @Schema(description = "Código de barras del producto", example = "7771234560011")
    @JsonProperty("codigoBarras")
    @JsonAlias({"codigoBarras", "codigo_barras"})
    private String codigoBarras;

    @Schema(description = "Cantidad vendida (decimal para permitir fracciones futuras)", example = "2")
    @JsonProperty("cantidad")
    private BigDecimal cantidad;

    @Schema(description = "Precio unitario congelado al momento de la venta (puede ser precio_venta o precio_descuento)", example = "10.50")
    @JsonProperty("precioUnitario")
    @JsonAlias({"precioUnitario", "precio_unitario"})
    private BigDecimal precioUnitario;

    @Schema(description = "Subtotal de la línea (cantidad * precioUnitario)", example = "21.00")
    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    // Aliases explícitos en getters para compatibilidad de serialización
    @JsonProperty("inventario_id")
    public Integer getInventario_id() {
        return inventarioId;
    }

    @JsonProperty("producto_id")
    public Integer getProducto_id() {
        return productoId;
    }

    @JsonProperty("precio_unitario")
    public BigDecimal getPrecio_unitario() {
        return precioUnitario;
    }
}
