package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorKpiDTO {

    @JsonProperty("totalProveedoresActivos")
    private Long totalProveedoresActivos;

    @JsonProperty("proveedoresAlertasVencimiento")
    private Long proveedoresAlertasVencimiento;

    @JsonProperty("totalProductosAbastecidos")
    private Long totalProductosAbastecidos;

    @JsonProperty("totalLotesActivos")
    private Long totalLotesActivos;

    @JsonProperty("valorTotalInventario")
    private BigDecimal valorTotalInventario;
}

