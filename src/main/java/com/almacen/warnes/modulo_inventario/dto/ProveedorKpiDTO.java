package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Métricas cuantitativas clave del módulo de distribuidores")
public class ProveedorKpiDTO {

    @Schema(description = "Total de empresas distribuidoras activas en el sistema", example = "6")
    @JsonProperty("totalProveedoresActivos")
    private Long totalProveedoresActivos;

    @Schema(description = "Cantidad de proveedores con lotes activos próximos a vencer en 7 días o menos", example = "1")
    @JsonProperty("proveedoresAlertasVencimiento")
    private Long proveedoresAlertasVencimiento;

    @Schema(description = "Total de productos distintos abastecidos por proveedores activos", example = "11")
    @JsonProperty("totalProductosAbastecidos")
    private Long totalProductosAbastecidos;

    @Schema(description = "Total de lotes activos con stock mayor a 0 pertenecientes a proveedores activos", example = "14")
    @JsonProperty("totalLotesActivos")
    private Long totalLotesActivos;

    @Schema(description = "Valorización monetaria total de la mercadería activa abastecida (stock * costo unitario)", example = "2295.90")
    @JsonProperty("valorTotalInventario")
    private BigDecimal valorTotalInventario;
}
