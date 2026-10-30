package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload para registrar o editar un ingreso de mercadería en la Sala de Espera")
public class RegistroPendienteRequestDTO {

    @Schema(description = "Identificador de la sucursal (por defecto 1)", example = "1")
    @JsonProperty("sucursalId")
    @JsonAlias({"sucursal_id", "id_sucursal"})
    @Builder.Default
    private Integer sucursalId = 1;

    @Schema(description = "Identificador del producto si ya existe en catálogo (opcional si es nuevo)", example = "1")
    @JsonProperty("productoId")
    @JsonAlias({"producto_id", "id_producto"})
    private Integer productoId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Schema(description = "Nombre descriptivo del producto a ingresar", example = "Leche Entera PIL Bolsa 946ml", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("nombreProducto")
    @JsonAlias({"nombre_producto", "nombre"})
    private String nombreProducto;

    @Schema(description = "Código de barras del empaque", example = "777123456001")
    @JsonProperty("codigoBarras")
    @JsonAlias({"codigo_barras"})
    private String codigoBarras;

    @NotBlank(message = "El código interno es obligatorio")
    @Schema(description = "Código interno asignado al producto (ej: LEC001)", example = "LEC001", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("codigoInterno")
    @JsonAlias({"codigo_interno"})
    private String codigoInterno;

    @NotNull(message = "La categoría es obligatoria")
    @Schema(description = "Identificador de categoría según Conceptos Grupo 2", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("categoriaId")
    @JsonAlias({"categoria_id", "id_categoria"})
    private Integer categoriaId;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @Schema(description = "Costo unitario de compra", example = "5.20", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("precioCompra")
    @JsonAlias({"precio_compra", "costo_unitario"})
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
    @Schema(description = "Precio de venta sugerido al público", example = "6.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("precioVenta")
    @JsonAlias({"precio_venta"})
    private BigDecimal precioVenta;

    @NotNull(message = "La cantidad o stock ingresado es obligatorio")
    @Min(value = 1, message = "El stock ingresado debe ser mayor a 0")
    @Schema(description = "Cantidad de unidades recibidas en el lote", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("stockIngresado")
    @JsonAlias({"stock_ingresado", "stock", "stock_actual"})
    private Integer stockIngresado;

    @Schema(description = "Identificador del proveedor distribuidor que abastece el lote", example = "1")
    @JsonProperty("proveedorId")
    @JsonAlias({"proveedor_id", "id_proveedor"})
    private Integer proveedorId;

    @Schema(description = "Descripción o identificación del lote", example = "Lote Leche Entrega Matutina")
    @JsonProperty("descripcionLote")
    @JsonAlias({"descripcion_lote", "descripcion"})
    private String descripcionLote;

    @Schema(description = "Precio total del lote facturado", example = "130.00")
    @JsonProperty("precioLote")
    @JsonAlias({"precio_lote"})
    private BigDecimal precioLote;

    @Schema(description = "Fecha de caducidad o vencimiento del lote", example = "2026-11-30")
    @JsonProperty("fechaVencimiento")
    @JsonAlias({"fecha_vencimiento"})
    private LocalDate fechaVencimiento;

    @Schema(description = "ID del usuario o cajero que registró el ingreso", example = "1")
    @JsonProperty("usuarioCreacion")
    @JsonAlias({"usuario_creacion", "id_usuario_creacion"})
    private Integer usuarioCreacion;

    @Schema(description = "ID del supervisor auditor que aprueba o rechaza", example = "2")
    @JsonProperty("usuarioRevision")
    @JsonAlias({"usuario_revision", "id_usuario_revision"})
    private Integer usuarioRevision;
}
