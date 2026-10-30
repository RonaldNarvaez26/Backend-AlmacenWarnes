package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroPendienteRequestDTO {

    @JsonProperty("sucursalId")
    @JsonAlias({"sucursal_id", "id_sucursal"})
    @Builder.Default
    private Integer sucursalId = 1;

    @JsonProperty("productoId")
    @JsonAlias({"producto_id", "id_producto"})
    private Integer productoId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @JsonProperty("nombreProducto")
    @JsonAlias({"nombre_producto", "nombre"})
    private String nombreProducto;

    @JsonProperty("codigoBarras")
    @JsonAlias({"codigo_barras"})
    private String codigoBarras;

    @NotBlank(message = "El código interno es obligatorio")
    @JsonProperty("codigoInterno")
    @JsonAlias({"codigo_interno"})
    private String codigoInterno;

    @NotNull(message = "La categoría es obligatoria")
    @JsonProperty("categoriaId")
    @JsonAlias({"categoria_id", "id_categoria"})
    private Integer categoriaId;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @JsonProperty("precioCompra")
    @JsonAlias({"precio_compra", "costo_unitario"})
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
    @JsonProperty("precioVenta")
    @JsonAlias({"precio_venta"})
    private BigDecimal precioVenta;

    @NotNull(message = "La cantidad o stock ingresado es obligatorio")
    @Min(value = 1, message = "El stock ingresado debe ser mayor a 0")
    @JsonProperty("stockIngresado")
    @JsonAlias({"stock_ingresado", "stock", "stock_actual"})
    private Integer stockIngresado;

    @JsonProperty("proveedorId")
    @JsonAlias({"proveedor_id", "id_proveedor"})
    private Integer proveedorId;

    @JsonProperty("descripcionLote")
    @JsonAlias({"descripcion_lote", "descripcion"})
    private String descripcionLote;

    @JsonProperty("precioLote")
    @JsonAlias({"precio_lote"})
    private BigDecimal precioLote;

    @JsonProperty("fechaVencimiento")
    @JsonAlias({"fecha_vencimiento"})
    private LocalDate fechaVencimiento;

    @JsonProperty("usuarioCreacion")
    @JsonAlias({"usuario_creacion", "id_usuario_creacion"})
    private Integer usuarioCreacion;

    @JsonProperty("usuarioRevision")
    @JsonAlias({"usuario_revision", "id_usuario_revision"})
    private Integer usuarioRevision;
}

