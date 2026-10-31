package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.projection.InventarioPendienteDetalleProjection;
import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventarioPendienteRepository extends JpaRepository<InventarioPendiente, Integer> {

    @Query("SELECT ip FROM InventarioPendiente ip WHERE ip.marcaBaja = false ORDER BY ip.inventarioPendienteId DESC")
    List<InventarioPendiente> findByMarcaBajaFalse();

    @Query("SELECT ip FROM InventarioPendiente ip WHERE ip.estado = :estado AND ip.marcaBaja = false ORDER BY ip.inventarioPendienteId DESC")
    List<InventarioPendiente> findByEstadoAndMarcaBajaFalse(@Param("estado") Integer estado);

    /**
     * Resuelve los datos de la Sala de Espera combinando la tabla inventario_pendientes con
     * el último lote conocido del producto mediante LEFT JOIN LATERAL, sin alterar el esquema de la BD.
     */
    @Query(value = """
        SELECT ip.inventario_pendiente_id AS inventarioPendienteId,
               ip.sucursal_id AS sucursalId,
               ip.producto_id AS productoId,
               ip.nombre_producto AS nombreProducto,
               ip.codigo_barras AS codigoBarras,
               ip.codigo_interno AS codigoInterno,
               ip.categoria_id AS categoriaId,
               ip.precio_compra AS precioCompra,
               ip.precio_venta AS precioVenta,
               ip.stock_ingresado AS stockIngresado,
               ip.estado AS estado,
               ip.marca_baja AS marcaBaja,
               ip.fecha_creacion AS fechaCreacion,
               ip.usuario_creacion AS usuarioCreacion,
               ip.fecha_revision AS fechaRevision,
               ip.usuario_revision AS usuarioRevision,
               ult_lote.proveedor_id AS proveedorId,
               ult_lote.descripcion AS descripcionLote,
               ult_lote.fecha_vencimiento AS fechaVencimiento
        FROM inventario_pendientes ip
        LEFT JOIN LATERAL (
            SELECT l.proveedor_id, l.descripcion, l.fecha_vencimiento
            FROM inventario inv
            JOIN lotes l ON inv.lote_id = l.lote_id
            WHERE inv.producto_id = ip.producto_id AND l.marca_baja = false
            ORDER BY l.lote_id DESC
            LIMIT 1
        ) ult_lote ON ip.producto_id IS NOT NULL
        WHERE (:estado IS NULL OR ip.estado = :estado) AND ip.marca_baja = false
        ORDER BY ip.inventario_pendiente_id DESC
    """, nativeQuery = true)
    List<InventarioPendienteDetalleProjection> findPendientesConDetalleLote(@Param("estado") Integer estado);

    @Query(value = """
        SELECT ip.inventario_pendiente_id AS inventarioPendienteId,
               ip.sucursal_id AS sucursalId,
               ip.producto_id AS productoId,
               ip.nombre_producto AS nombreProducto,
               ip.codigo_barras AS codigoBarras,
               ip.codigo_interno AS codigoInterno,
               ip.categoria_id AS categoriaId,
               ip.precio_compra AS precioCompra,
               ip.precio_venta AS precioVenta,
               ip.stock_ingresado AS stockIngresado,
               ip.estado AS estado,
               ip.marca_baja AS marcaBaja,
               ip.fecha_creacion AS fechaCreacion,
               ip.usuario_creacion AS usuarioCreacion,
               ip.fecha_revision AS fechaRevision,
               ip.usuario_revision AS usuarioRevision,
               ult_lote.proveedor_id AS proveedorId,
               ult_lote.descripcion AS descripcionLote,
               ult_lote.fecha_vencimiento AS fechaVencimiento
        FROM inventario_pendientes ip
        LEFT JOIN LATERAL (
            SELECT l.proveedor_id, l.descripcion, l.fecha_vencimiento
            FROM inventario inv
            JOIN lotes l ON inv.lote_id = l.lote_id
            WHERE inv.producto_id = ip.producto_id AND l.marca_baja = false
            ORDER BY l.lote_id DESC
            LIMIT 1
        ) ult_lote ON ip.producto_id IS NOT NULL
        WHERE ip.marca_baja = false
        ORDER BY ip.inventario_pendiente_id DESC
    """, nativeQuery = true)
    List<InventarioPendienteDetalleProjection> findAllConDetalleLote();
}
