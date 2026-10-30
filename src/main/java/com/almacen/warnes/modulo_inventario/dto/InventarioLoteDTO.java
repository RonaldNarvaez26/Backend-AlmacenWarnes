package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioLoteDTO {

    @JsonProperty("inventarioId")
    private Integer inventarioId;

    @JsonProperty("stock")
    private Integer stock;

    @JsonProperty("enDescuento")
    private Boolean enDescuento;

    @JsonProperty("precioDescuento")
    private BigDecimal precioDescuento;

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("estadoProducto")
    private Integer estadoProducto;

    @JsonProperty("loteId")
    private Integer loteId;

    @JsonProperty("descripcionLote")
    private String descripcionLote;

    @JsonProperty("costoUnitario")
    private BigDecimal costoUnitario;

    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @JsonProperty("nombreProveedor")
    private String nombreProveedor;

    @JsonProperty("diasParaVencer")
    private Integer diasParaVencer;

    @JsonProperty("inventario_id")
    public Integer getInventario_id() {
        return inventarioId;
    }

    @JsonProperty("en_descuento")
    public Boolean getEn_descuento() {
        return enDescuento;
    }

    @JsonProperty("precio_descuento")
    public BigDecimal getPrecio_descuento() {
        return precioDescuento;
    }

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

    @JsonProperty("precio_venta")
    public BigDecimal getPrecio_venta() {
        return precioVenta;
    }

    @JsonProperty("estado_producto")
    public Integer getEstado_producto() {
        return estadoProducto;
    }

    @JsonProperty("lote_id")
    public Integer getLote_id() {
        return loteId;
    }

    @JsonProperty("descripcion_lote")
    public String getDescripcion_lote() {
        return descripcionLote;
    }

    @JsonProperty("costo_unitario")
    public BigDecimal getCosto_unitario() {
        return costoUnitario;
    }

    @JsonProperty("fecha_vencimiento")
    public LocalDate getFecha_vencimiento() {
        return fechaVencimiento;
    }

    @JsonProperty("nombre_proveedor")
    public String getNombre_proveedor() {
        return nombreProveedor;
    }

    @JsonProperty("dias_para_vencer")
    public Integer getDias_para_vencer() {
        return diasParaVencer;
    }
}

