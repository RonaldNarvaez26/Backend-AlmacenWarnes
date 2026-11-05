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
@Schema(description = "Datos detallados de un distribuidor con métricas consolidadas de lotes y productos")
public class ProveedorListadoDTO {

    @Schema(description = "Identificador único del distribuidor", example = "1")
    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @Schema(description = "Razón social de la empresa", example = "PIL Andina S.A.")
    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @Schema(description = "Contacto logístico o preventista", example = "Roberto Gómez")
    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @Schema(description = "Número telefónico del contacto", example = "70112233")
    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @Schema(description = "Acuerdos contractuales y términos logísticos", example = "Contrato CONTR-PIL-2026: Entregas martes y viernes 08:00 AM. Crédito quincenal.")
    @JsonProperty("anotaciones")
    private String anotaciones;

    @Schema(description = "Estado de baja lógica del distribuidor", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @Schema(description = "Fecha y hora de registro en el sistema", example = "2026-10-29T13:21:35")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Cantidad total de productos distintos que este proveedor abastece", example = "4")
    @JsonProperty("totalProductos")
    private Long totalProductos;

    @Schema(description = "Total de lotes históricos entregados", example = "6")
    @JsonProperty("totalLotes")
    private Long totalLotes;

    @Schema(description = "Lotes vigentes con stock activo mayor a 0", example = "6")
    @JsonProperty("lotesActivos")
    private Long lotesActivos;

    @Schema(description = "Lotes activos con riesgo de vencimiento en 7 días o menos", example = "3")
    @JsonProperty("lotesEnRiesgo")
    private Long lotesEnRiesgo;

    @Schema(description = "Stock físico total acumulado en almacén proveniente de este distribuidor", example = "106")
    @JsonProperty("stockTotalAcumulado")
    private Long stockTotalAcumulado;

    @Schema(description = "Valor total monetario de la mercadería activa abastecida", example = "668.40")
    @JsonProperty("valorMercaderia")
    private BigDecimal valorMercaderia;
}
