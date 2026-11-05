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
@Schema(description = "Datos completos de la entidad Producto en catálogo")
public class ProductoDTO {

    @Schema(description = "Identificador único del producto", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "ID de categoría (Conceptos Grupo 2)", example = "1")
    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @Schema(description = "Código de barras", example = "777123456001")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Nombre comercial", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Código interno único (6 caracteres)", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Precio de compra", example = "5.20")
    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @Schema(description = "Precio de venta", example = "6.00")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Estado del producto (1: Activo, 2: Descontinuado, 3: Suspendido)", example = "1")
    @JsonProperty("estado")
    private Integer estado;

    @Schema(description = "Marca de baja lógica", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @Schema(description = "Fecha de creación", example = "2026-10-29T13:21:35")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "ID usuario creador", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @Schema(description = "Fecha de última modificación", example = "2026-10-29T15:30:00")
    @JsonProperty("fechaModificacion")
    private LocalDateTime fechaModificacion;

    @Schema(description = "ID usuario modificador", example = "1")
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
