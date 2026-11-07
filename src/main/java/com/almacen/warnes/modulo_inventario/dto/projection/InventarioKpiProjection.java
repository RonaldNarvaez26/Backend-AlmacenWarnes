package com.almacen.warnes.modulo_inventario.dto.projection;

public interface InventarioKpiProjection {
    Long getTotal_productos_catalogo();
    Long getUnidades_stock_activo();
    Long getLotes_sala_espera();
    Long getAlertas_vencimiento();

    default Long getTotalProductosCatalogo() { return getTotal_productos_catalogo(); }
    default Long getUnidadesStockActivo() { return getUnidades_stock_activo(); }
    default Long getLotesSalaEspera() { return getLotes_sala_espera(); }
    default Long getAlertasVencimiento() { return getAlertas_vencimiento(); }
}

