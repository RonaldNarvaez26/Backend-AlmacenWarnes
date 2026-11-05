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
@Schema(description = "Detalle de lote entregado por un distribuidor con estado de caducidad")
public class ProveedorHistorialLoteDTO {

    @Schema(description = "Identificador del lote", example = "1")
    @JsonProperty("loteId")
    private Integer loteId;

    @Schema(description = "Descripción o glosa del lote", example = "Lote Leche PIL (Alerta vence en 2 dias)")
    @JsonProperty("descripcion")
    private String descripcion;

    @Schema(description = "Identificador del producto vinculado", example = "1")
    @JsonProperty("productoId")
    private Integer productoId;

    @Schema(description = "Nombre del producto", example = "Leche Entera PIL Bolsa 946ml")
    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @Schema(description = "Código interno del producto", example = "LEC001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Cantidad de unidades ingresadas inicialmente", example = "20")
    @JsonProperty("cantidadInicial")
    private Integer cantidadInicial;

    @Schema(description = "Unidades actualmente disponibles en stock", example = "8")
    @JsonProperty("stockActual")
    private Integer stockActual;

    @Schema(description = "Costo unitario de compra", example = "5.20")
    @JsonProperty("costoUnitario")
    private BigDecimal costoUnitario;

    @Schema(description = "Precio total del lote recibido", example = "104.00")
    @JsonProperty("precioLote")
    private BigDecimal precioLote;

    @Schema(description = "Fecha de vencimiento o caducidad", example = "2026-10-31")
    @JsonProperty("fechaVencimiento")
    private LocalDate fechaVencimiento;

    @Schema(description = "Días restantes para vencer (negativo si está vencido)", example = "1")
    @JsonProperty("diasParaVencer")
    private Integer diasParaVencer;

    @Schema(description = "Clasificación de caducidad (CRITICO, PROXIMO, VIGENTE, VENCIDO, NO_PERECEDERO)", example = "CRITICO")
    @JsonProperty("estadoCaducidad")
    private String estadoCaducidad;

    @Schema(description = "Fecha y hora de registro de ingreso", example = "2026-10-29T13:21:35")
    @JsonProperty("fechaIngreso")
    private LocalDateTime fechaIngreso;
}
