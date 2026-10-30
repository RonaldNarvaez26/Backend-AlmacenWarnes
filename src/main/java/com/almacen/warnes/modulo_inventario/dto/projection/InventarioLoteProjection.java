package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InventarioLoteProjection {
    Integer getInventario_id();
    Integer getStock();
    Boolean getEn_descuento();
    BigDecimal getPrecio_descuento();
    Integer getProducto_id();
    String getCodigo_interno();
    String getCodigo_barras();
    String getNombre_producto();
    BigDecimal getPrecio_venta();
    Integer getEstado_producto();
    Integer getLote_id();
    String getDescripcion_lote();
    BigDecimal getCosto_unitario();
    LocalDate getFecha_vencimiento();
    String getNombre_proveedor();
    Integer getDias_para_vencer();
}

