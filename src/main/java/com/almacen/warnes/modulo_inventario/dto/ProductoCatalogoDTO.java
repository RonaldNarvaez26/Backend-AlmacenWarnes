package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Producto maestro del catálogo comercial con stock físico total consolidado")
public class ProductoCatalogoDTO {

    @Schema(description = "Identificador único del producto", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Código interno de 6 caracteres", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Código de barras para pistola lectora", example = "777123456001")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Nombre o denominación del producto", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Código de la categoría (Grupo 2 en Conceptos)", example = "1")
    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @Schema(description = "Precio de adquisición base", example = "5.20")
    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @Schema(description = "Precio de venta al público", example = "6.00")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Estado en catálogo (1: Activo, 2: Descontinuado, 3: Suspendido)", example = "1")
    @JsonProperty("estado")
    private Integer estado;

    @Schema(description = "Suma total de unidades físicas en inventario activo a través de todos los lotes", example = "78")
    @JsonProperty("stockTotalAcumulado")
    private Long stockTotalAcumulado;

    @JsonProperty("producto_id")
    public Integer getProducto_id() {
        return productoId;
    }

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() {
        return codigoInterno;
    }

    @JsonProperty("codigo_barras")
    public String getCodigo_barras() {
        return codigoBarras;
    }

    @JsonProperty("nombre_producto")
    public String getNombre_producto() {
        return nombreProducto;
    }

    @JsonProperty("categoria_id")
    public Integer getCategoria_id() {
        return categoriaId;
    }

    @JsonProperty("precio_compra")
    public BigDecimal getPrecio_compra() {
        return precioCompra;
    }

    @JsonProperty("precio_venta")
    public BigDecimal getPrecio_venta() {
        return precioVenta;
    }

    @JsonProperty("stock_total_acumulado")
    public Long getStock_total_acumulado() {
        return stockTotalAcumulado;
    }
}
