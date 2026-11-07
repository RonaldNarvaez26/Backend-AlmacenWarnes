package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface InventarioPendienteDetalleProjection {
    Integer getInventarioPendienteId();
    Integer getSucursalId();
    String getCodigoSucursal();
    String getCiudadSucursal();
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

    // Campos del lote resueltos directamente desde columnas físicas y JOINs
    Integer getProveedorId();
    String getDescripcionLote();
    LocalDate getFechaVencimiento();
    String getObservacionRevision();
    LocalDateTime getFechaSubsanacion();
    String getNombreUsuarioCreacion();
    String getNombre_usuario_creacion();
    String getNombreUsuarioRevision();
    String getNombre_usuario_revision();
    String getRolUsuarioCreacion();
    String getRol_usuario_creacion();
    String getNombreProveedor();
    String getNombre_proveedor();
}

