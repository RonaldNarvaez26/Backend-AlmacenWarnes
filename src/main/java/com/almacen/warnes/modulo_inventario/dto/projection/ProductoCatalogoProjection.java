package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;

public interface ProductoCatalogoProjection {
    Integer getProducto_id();
    String getCodigo_interno();
    String getCodigo_barras();
    String getNombre_producto();
    Integer getCategoria_id();
    BigDecimal getPrecio_compra();
    BigDecimal getPrecio_venta();
    Integer getEstado();
    Long getStock_total_acumulado();
}

