package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProveedorHistorialLoteProjection {
    Integer getLote_id();
    String getDescripcion();
    Integer getProducto_id();
    String getNombre_producto();
    String getCodigo_interno();
    Integer getCantidad_inicial();
    Integer getStock_actual();
    BigDecimal getCosto_unitario();
    BigDecimal getPrecio_lote();
    LocalDate getFecha_vencimiento();
    Integer getDias_para_vencer();
    String getEstado_caducidad();
    LocalDateTime getFecha_ingreso();
}

