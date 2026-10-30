package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("estado")
    private Integer estado;

    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @JsonProperty("fechaModificacion")
    private LocalDateTime fechaModificacion;

    @JsonProperty("usuarioModificacion")
    private Integer usuarioModificacion;

    @JsonProperty("producto_id")
    public Integer getProducto_id() { return productoId; }

    @JsonProperty("categoria_id")
    public Integer getCategoria_id() { return categoriaId; }

    @JsonProperty("codigo_barras")
    public String getCodigo_barras() { return codigoBarras; }

    @JsonProperty("nombre_producto")
    public String getNombre_producto() { return nombreProducto; }

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() { return codigoInterno; }

    @JsonProperty("precio_compra")
    public BigDecimal getPrecio_compra() { return precioCompra; }

    @JsonProperty("precio_venta")
    public BigDecimal getPrecio_venta() { return precioVenta; }

    @JsonProperty("marca_baja")
    public Boolean getMarca_baja() { return marcaBaja; }
}
