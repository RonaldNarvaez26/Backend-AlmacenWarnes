package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Métricas cuantitativas clave del inventario general en tiempo real")
public class InventarioKpiDTO {

    @Schema(description = "Total de definiciones maestras de productos activos en el catálogo", example = "11")
    @JsonProperty("totalProductosCatalogo")
    private Long totalProductosCatalogo;

    @Schema(description = "Total de unidades físicas en stock activo en almacén", example = "247")
    @JsonProperty("unidadesStockActivo")
    private Long unidadesStockActivo;

    @Schema(description = "Cantidad de ingresos de mercadería esperando revisión en la sala de espera", example = "0")
    @JsonProperty("lotesSalaEspera")
    private Long lotesSalaEspera;

    @Schema(description = "Lotes activos con alerta de caducidad próxima", example = "3")
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
