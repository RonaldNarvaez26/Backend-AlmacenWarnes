package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioPendienteDTO {

    @JsonProperty("inventarioPendienteId")
    private Integer inventarioPendienteId;

    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("stockIngresado")
    private Integer stockIngresado;

    @JsonProperty("estado")
    private Integer estado;

    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @JsonProperty("descripcionLote")
    private String descripcionLote;

    @JsonProperty("precioLote")
    private BigDecimal precioLote;

    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @JsonProperty("fechaRevision")
    private LocalDateTime fechaRevision;

    @JsonProperty("usuarioRevision")
    private Integer usuarioRevision;

    @JsonProperty("inventario_pendiente_id")
    public Integer getInventario_pendiente_id() { return inventarioPendienteId; }

    @JsonProperty("sucursal_id")
    public Integer getSucursal_id() { return sucursalId; }

    @JsonProperty("producto_id")
    public Integer getProducto_id() { return productoId; }

    @JsonProperty("nombre_producto")
    public String getNombre_producto() { return nombreProducto; }

    @JsonProperty("codigo_barras")
    public String getCodigo_barras() { return codigoBarras; }

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() { return codigoInterno; }

    @JsonProperty("categoria_id")
    public Integer getCategoria_id() { return categoriaId; }

    @JsonProperty("precio_compra")
    public BigDecimal getPrecio_compra() { return precioCompra; }

    @JsonProperty("precio_venta")
    public BigDecimal getPrecio_venta() { return precioVenta; }

    @JsonProperty("stock_ingresado")
    public Integer getStock_ingresado() { return stockIngresado; }

    @JsonProperty("proveedor_id")
    public Integer getProveedor_id() { return proveedorId; }

    @JsonProperty("descripcion_lote")
    public String getDescripcion_lote() { return descripcionLote; }

    @JsonProperty("precio_lote")
    public BigDecimal getPrecio_lote() { return precioLote; }

    @JsonProperty("fecha_vencimiento")
    public LocalDate getFecha_vencimiento() { return fechaVencimiento; }

    @JsonProperty("usuario_creacion")
    public Integer getUsuario_creacion() { return usuarioCreacion; }

    @JsonProperty("usuario_revision")
    public Integer getUsuario_revision() { return usuarioRevision; }
}

