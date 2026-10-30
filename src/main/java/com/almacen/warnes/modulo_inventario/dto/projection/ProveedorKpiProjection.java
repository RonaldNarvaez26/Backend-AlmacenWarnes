package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;

public interface ProveedorKpiProjection {
    Long getTotal_proveedores_activos();
    Long getProveedores_alertas_vencimiento();
    Long getTotal_productos_abastecidos();
    Long getTotal_lotes_activos();
    BigDecimal getValor_total_inventario();
}

