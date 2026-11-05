package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de inventario físico activo asociado a un lote, producto y proveedor")
public class InventarioLoteDTO {

    @Schema(description = "Identificador único en inventario físico", example = "1")
    @JsonProperty("inventarioId")
    private Integer inventarioId;

    @Schema(description = "Unidades físicas disponibles en stock", example = "8")
    @JsonProperty("stock")
    private Integer stock;

    @Schema(description = "Bandera que indica si el lote tiene precio de oferta/descuento activo", example = "false")
    @JsonProperty("enDescuento")
    private Boolean enDescuento;

    @Schema(description = "Precio especial de descuento para acelerar rotación", example = "5.00")
    @JsonProperty("precioDescuento")
    private BigDecimal precioDescuento;

    @Schema(description = "Identificador del producto en catálogo", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Código interno del producto", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Código de barras del producto", example = "777123456001")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Nombre comercial del producto", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Precio normal de venta al público", example = "6.00")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Estado del producto (1: Activo, 2: Descontinuado, 3: Suspendido)", example = "1")
    @JsonProperty("estadoProducto")
    private Integer estadoProducto;

    @Schema(description = "Identificador del lote", example = "1")
    @JsonProperty("loteId")
    private Integer loteId;

    @Schema(description = "Descripción o glosa del lote recibido", example = "Lote Leche PIL (Alerta vence en 2 dias)")
    @JsonProperty("descripcionLote")
    private String descripcionLote;

    @Schema(description = "Costo unitario de compra", example = "5.20")
    @JsonProperty("costoUnitario")
    private BigDecimal costoUnitario;

    @Schema(description = "Fecha de vencimiento o caducidad", example = "2026-10-31")
    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @Schema(description = "Nombre de la empresa distribuidora abastecedora", example = "PIL Andina S.A.")
    @JsonProperty("nombreProveedor")
    private String nombreProveedor;

    @Schema(description = "Días restantes para caducar (negativo si vencido)", example = "1")
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
