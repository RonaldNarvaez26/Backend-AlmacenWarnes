package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface InventarioPendienteDetalleProjection {
    Integer getInventarioPendienteId();
    Integer getSucursalId();
    Integer getProductoId();
    String getNombreProducto();
    String getCodigoBarras();
    String getCodigoInterno();
    Integer getCategoriaId();
    BigDecimal getPrecioCompra();
    BigDecimal getPrecioVenta();
    Integer getStockIngresado();
    Integer getEstado();
    Boolean getMarcaBaja();
    LocalDateTime getFechaCreacion();
    Integer getUsuarioCreacion();
    LocalDateTime getFechaRevision();
    Integer getUsuarioRevision();

    // Campos del lote resueltos vía JOIN a nivel de consulta
    Integer getProveedorId();
    String getDescripcionLote();
    LocalDate getFechaVencimiento();
}

