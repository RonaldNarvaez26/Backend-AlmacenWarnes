package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de mercadería en Sala de Espera para auditoría y aprobación")
public class InventarioPendienteDTO {

    @Schema(description = "Identificador único en inventario_pendientes", example = "101")
    @JsonProperty("inventarioPendienteId")
    private Integer inventarioPendienteId;

    @Schema(description = "ID de la sucursal", example = "1")
    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @Schema(description = "ID del producto vinculado en catálogo (si existe)", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Nombre descriptivo del producto ingresado", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Código de barras", example = "777123456001")
    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @Schema(description = "Código interno asignado", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Categoría asignada según Conceptos", example = "1")
    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @Schema(description = "Costo unitario de compra", example = "5.20")
    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @Schema(description = "Precio de venta sugerido", example = "6.00")
    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @Schema(description = "Cantidad de unidades recibidas", example = "30")
    @JsonProperty("stockIngresado")
    private Integer stockIngresado;

    @Schema(description = "Estado de revisión (1: Pendiente, 2: Aprobado, 3: Rechazado, 4: Observado / Para Corrección)", example = "1")
    @JsonProperty("estado")
    private Integer estado;

    @Schema(description = "ID del proveedor abastecedor asociado en tránsito", example = "1")
    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @Schema(description = "Descripción o código de lote", example = "Recepcion Turno Manana Camion PIL")
    @JsonProperty("descripcionLote")
    private String descripcionLote;

    @Schema(description = "Precio total del lote", example = "156.00")
    @JsonProperty("precioLote")
    private BigDecimal precioLote;

    @Schema(description = "Fecha de vencimiento declarada para el lote", example = "2026-12-08")
    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @Schema(description = "Observación o instrucción de corrección redactada por el supervisor", example = "Verificar precio de compra con factura física #4502")
    @JsonProperty("observacionRevision")
    private String observacionRevision;

    @Schema(description = "Baja lógica", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @Schema(description = "Fecha de registro por el cajero", example = "2026-10-29T13:28:59")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "ID del usuario cajero creador", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @Schema(description = "Fecha de revisión o auditoría", example = "2026-10-29T14:00:00")
    @JsonProperty("fechaRevision")
    private LocalDateTime fechaRevision;

    @Schema(description = "ID del supervisor auditor", example = "2")
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

    @JsonProperty("observacion_revision")
    public String getObservacion_revision() { return observacionRevision; }

    @JsonProperty("observacion_revision")
    public void setObservacion_revision(String obs) { this.observacionRevision = obs; }

    @JsonProperty("usuario_creacion")
    public Integer getUsuario_creacion() { return usuarioCreacion; }

    @JsonProperty("usuario_revision")
    public Integer getUsuario_revision() { return usuarioRevision; }
}
