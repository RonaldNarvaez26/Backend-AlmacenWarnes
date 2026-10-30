package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioKpiDTO {

    @JsonProperty("totalProductosCatalogo")
    private Long totalProductosCatalogo;

    @JsonProperty("unidadesStockActivo")
    private Long unidadesStockActivo;

    @JsonProperty("lotesSalaEspera")
    private Long lotesSalaEspera;

    @JsonProperty("alertasVencimiento")
    private Long alertasVencimiento;

    @JsonProperty("total_productos_catalogo")
    public Long getTotal_productos_catalogo() {
        return totalProductosCatalogo;
    }

    @JsonProperty("unidades_stock_activo")
    public Long getUnidades_stock_activo() {
        return unidadesStockActivo;
    }

    @JsonProperty("lotes_sala_espera")
    public Long getLotes_sala_espera() {
        return lotesSalaEspera;
    }

    @JsonProperty("alertas_vencimiento")
    public Long getAlertas_vencimiento() {
        return alertasVencimiento;
    }
}

