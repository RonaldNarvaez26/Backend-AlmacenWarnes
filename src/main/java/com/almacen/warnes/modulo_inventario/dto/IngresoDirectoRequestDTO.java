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
@Schema(description = "Payload para registro e ingreso directo de mercadería por el Supervisor (spInventarioIngresoDirectoSupervisor)")
public class IngresoDirectoRequestDTO {

    @NotNull(message = "El campo categoriaId es obligatorio")
    @Schema(description = "ID de la categoría del producto (Conceptos Grupo 2)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("categoriaId")
    @JsonAlias({"categoria_id", "id_categoria"})
    private Integer categoriaId;

    @NotBlank(message = "El campo nombreProducto es obligatorio")
    @Schema(description = "Nombre o denominación del producto", example = "Leche Deslactosada PIL 946ml", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("nombreProducto")
    @JsonAlias({"nombre_producto", "nombre"})
    private String nombreProducto;

    @NotBlank(message = "El campo codigoInterno es obligatorio")
    @Schema(description = "Código interno de 6 caracteres", example = "LEC002", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("codigoInterno")
    @JsonAlias({"codigo_interno"})
    private String codigoInterno;

    @Schema(description = "Descripción o glosa del lote recibido", example = "Lote Deslactosada Reposición Quincenal")
    @JsonProperty("descripcionLote")
    @JsonAlias({"descripcion_lote", "descripcion"})
    private String descripcionLote;

    @NotNull(message = "El campo stockIngresado es obligatorio")
    @Min(value = 1, message = "El stock ingresado debe ser mayor a 0")
    @Schema(description = "Cantidad de unidades que ingresan inmediatamente al stock físico", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("stockIngresado")
    @JsonAlias({"stock_ingresado", "stock", "stock_actual"})
    private Integer stockIngresado;

    @Schema(description = "Costo total facturado del lote (calculado automáticamente si es omitido)", example = "290.00")
    @JsonProperty("precioLote")
    @JsonAlias({"precio_lote"})
    private BigDecimal precioLote;

    @NotNull(message = "El campo precioCompra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @Schema(description = "Costo unitario de compra", example = "5.80", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("precioCompra")
    @JsonAlias({"precio_compra", "costo_unitario"})
    private BigDecimal precioCompra;

    @NotNull(message = "El campo precioVenta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
    @Schema(description = "Precio de venta al público", example = "7.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("precioVenta")
    @JsonAlias({"precio_venta"})
    private BigDecimal precioVenta;

    @Schema(description = "ID del usuario supervisor que autoriza el ingreso directo", example = "2")
    @JsonProperty("usuarioCreacion")
    @JsonAlias({"usuario_creacion", "id_usuario_creacion"})
    private Integer usuarioCreacion;

    @Schema(description = "ID del producto existente si ya se encuentra en catálogo", example = "2")
    @JsonProperty("productoId")
    @JsonAlias({"producto_id", "id_producto"})
    private Integer productoId;

    @Schema(description = "Código de barras para pistola lectora", example = "777123456002")
    @JsonProperty("codigoBarras")
    @JsonAlias({"codigo_barras"})
    private String codigoBarras;

    @Schema(description = "ID del proveedor abastecedor del lote", example = "1")
    @JsonProperty("proveedorId")
    @JsonAlias({"proveedor_id", "id_proveedor"})
    private Integer proveedorId;

    @Schema(description = "Fecha de vencimiento declarada para el lote", example = "2026-12-15")
    @JsonProperty("fechaVencimiento")
    @JsonAlias({"fecha_vencimiento"})
    private LocalDate fechaVencimiento;
}
