package com.almacen.warnes.modulo_inventario.dto.projection;

import java.time.LocalDateTime;

public interface HistorialInventarioProjection {
    Integer getHistorial_id();
    Integer getSucursal_id();
    String getCodigo_sucursal();
    String getCiudad_sucursal();
    Integer getProducto_id();
    String getCodigo_interno();
    String getCodigo_barras();
    String getNombre_producto();
    Integer getLote_id();
    String getDescripcion_lote();
    Integer getTipo_movimiento();
    String getNombre_tipo_movimiento();
    Integer getStock_anterior();
    Integer getCantidad();
    Integer getStock_resultante();
    String getDescripcion();
    Boolean getMarca_baja();
    LocalDateTime getFecha_creacion();
    Integer getUsuario_creacion();
    String getNombre_usuario_creacion();
    Integer getUsuario_autorizacion();
    String getNombre_usuario_autorizacion();
}

