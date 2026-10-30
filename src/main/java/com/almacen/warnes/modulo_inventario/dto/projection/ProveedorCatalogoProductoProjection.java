package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProveedorCatalogoProductoProjection {
    Integer getProducto_id();
    String getCodigo_interno();
    String getCodigo_barras();
    String getNombre_producto();
    String getCategoria_nombre();
    BigDecimal getPrecio_compra();
    BigDecimal getPrecio_venta();
    Integer getEstado_producto();
    Long getStock_actual();
    Long getTotal_lotes_recibidos();
    LocalDateTime getFecha_ultima_entrega();
}

