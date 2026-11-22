package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de trazabilidad y kardex de movimientos de inventario")
public class HistorialInventarioDTO {

    @Schema(description = "Identificador único del registro de kardex", example = "1")
    @JsonProperty("historialId")
    private Integer historialId;

    @Schema(description = "Identificador de la sucursal", example = "1")
    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @Schema(description = "Código de la sucursal", example = "SUC-001")
    @JsonProperty("codigoSucursal")
    private String codigoSucursal;

    @Schema(description = "Ciudad o ubicación de la sucursal", example = "Warnes Central")
    @JsonProperty("ciudadSucursal")
    private String ciudadSucursal;

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

    @Schema(description = "Identificador del lote involucrado", example = "2")
    @JsonProperty("loteId")
    private Integer loteId;

    @Schema(description = "Descripción o código de lote", example = "Lote Leche PIL #1")
    @JsonProperty("descripcionLote")
    private String descripcionLote;

    @Schema(description = "Código numérico de tipo de movimiento (1=Ingreso, 2=Egreso, 3=Ajuste)", example = "1")
    @JsonProperty("tipoMovimiento")
    private Integer tipoMovimiento;

    @Schema(description = "Nombre descriptivo del tipo de movimiento", example = "Ingreso")
    @JsonProperty("nombreTipoMovimiento")
    private String nombreTipoMovimiento;

    @Schema(description = "Stock del producto en la sucursal antes del movimiento", example = "0")
    @JsonProperty("stockAnterior")
    private Integer stockAnterior;

    @Schema(description = "Cantidad de unidades involucradas en el movimiento", example = "20")
    @JsonProperty("cantidad")
    private Integer cantidad;

    @Schema(description = "Stock resultante tras la aplicación del movimiento", example = "20")
    @JsonProperty("stockResultante")
    private Integer stockResultante;

    @Schema(description = "Descripción o motivo del movimiento", example = "Ingreso directo de lote")
    @JsonProperty("descripcion")
    private String descripcion;

    @Schema(description = "Fecha y hora exacta en que se registró el movimiento", example = "2026-11-08T18:00:00")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "ID del usuario que solicitó o registró el movimiento", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @Schema(description = "Nombre completo del usuario que registró el movimiento", example = "Carlos Eduardo Mendoza")
    @JsonProperty("nombreUsuarioCreacion")
    private String nombreUsuarioCreacion;

    @Schema(description = "ID del usuario supervisor que autorizó el movimiento", example = "1")
    @JsonProperty("usuarioAutorizacion")
    private Integer usuarioAutorizacion;

    @Schema(description = "Nombre completo del usuario que autorizó el movimiento", example = "Carlos Eduardo Mendoza")
    @JsonProperty("nombreUsuarioAutorizacion")
    private String nombreUsuarioAutorizacion;
}

