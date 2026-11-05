package com.almacen.warnes.modulo_inventario.dto.projection;

public interface InventarioKpiProjection {
    Long getTotal_productos_catalogo();
    Long getUnidades_stock_activo();
    Long getLotes_sala_espera();
    Long getAlertas_vencimiento();
}

