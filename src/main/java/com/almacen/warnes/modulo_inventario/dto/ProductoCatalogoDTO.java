package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCatalogoDTO {

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("estado")
    private Integer estado;

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

