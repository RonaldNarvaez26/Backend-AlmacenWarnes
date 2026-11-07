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
     * Resuelve los datos de la Sala de Espera leyendo directamente las columnas físicas
     * de inventario_pendientes y cruzando con sucursales, usuarios y proveedores.
     * Soporta aislamiento por sucursal (p_sucursal_id).
     */
    @Query(value = """
        SELECT ip.inventario_pendiente_id AS inventarioPendienteId,
               ip.sucursal_id AS sucursalId,
               s.codigo_interno AS codigoSucursal,
               s.ciudad AS ciudadSucursal,
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
               ip.proveedor_id AS proveedorId,
               ip.descripcion_lote AS descripcionLote,
               ip.fecha_vencimiento AS fechaVencimiento,
               ip.observacion_revision AS observacionRevision,
               ip.fecha_subsanacion AS fechaSubsanacion,
               TRIM(CONCAT(COALESCE(uc.nombres, ''), ' ', COALESCE(uc.apellidos, ''))) AS "nombreUsuarioCreacion",
               TRIM(CONCAT(COALESCE(uc.nombres, ''), ' ', COALESCE(uc.apellidos, ''))) AS "nombre_usuario_creacion",
               TRIM(CONCAT(COALESCE(ur.nombres, ''), ' ', COALESCE(ur.apellidos, ''))) AS "nombreUsuarioRevision",
               TRIM(CONCAT(COALESCE(ur.nombres, ''), ' ', COALESCE(ur.apellidos, ''))) AS "nombre_usuario_revision",
               c_rol.descripcion AS "rolUsuarioCreacion",
               c_rol.descripcion AS "rol_usuario_creacion",
               pr.nombre_empresa AS "nombreProveedor",
               pr.nombre_empresa AS "nombre_proveedor"
        FROM inventario_pendientes ip
        LEFT JOIN sucursales s ON s.sucursal_id = ip.sucursal_id
        LEFT JOIN usuarios uc ON uc.usuario_id = ip.usuario_creacion
        LEFT JOIN conceptos c_rol ON c_rol.grupo_id = 1 AND c_rol.codigo = uc.rol_id AND c_rol.marca_baja = false
        LEFT JOIN usuarios ur ON ur.usuario_id = ip.usuario_revision
        LEFT JOIN proveedores pr ON pr.proveedor_id = ip.proveedor_id
        WHERE (:sucursalId IS NULL OR ip.sucursal_id = :sucursalId)
          AND (:estado IS NULL OR ip.estado = :estado)
          AND ip.marca_baja = false
        ORDER BY ip.inventario_pendiente_id DESC
    """, nativeQuery = true)
    List<InventarioPendienteDetalleProjection> findPendientesConDetalleLote(
            @Param("estado") Integer estado,
            @Param("sucursalId") Integer sucursalId
    );

    @Query(value = """
        SELECT ip.inventario_pendiente_id AS inventarioPendienteId,
               ip.sucursal_id AS sucursalId,
               s.codigo_interno AS codigoSucursal,
               s.ciudad AS ciudadSucursal,
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
               ip.proveedor_id AS proveedorId,
               ip.descripcion_lote AS descripcionLote,
               ip.fecha_vencimiento AS fechaVencimiento,
               ip.observacion_revision AS observacionRevision,
               ip.fecha_subsanacion AS fechaSubsanacion,
               TRIM(CONCAT(COALESCE(uc.nombres, ''), ' ', COALESCE(uc.apellidos, ''))) AS "nombreUsuarioCreacion",
               TRIM(CONCAT(COALESCE(uc.nombres, ''), ' ', COALESCE(uc.apellidos, ''))) AS "nombre_usuario_creacion",
               TRIM(CONCAT(COALESCE(ur.nombres, ''), ' ', COALESCE(ur.apellidos, ''))) AS "nombreUsuarioRevision",
               TRIM(CONCAT(COALESCE(ur.nombres, ''), ' ', COALESCE(ur.apellidos, ''))) AS "nombre_usuario_revision",
               c_rol.descripcion AS "rolUsuarioCreacion",
               c_rol.descripcion AS "rol_usuario_creacion",
               pr.nombre_empresa AS "nombreProveedor",
               pr.nombre_empresa AS "nombre_proveedor"
        FROM inventario_pendientes ip
        LEFT JOIN sucursales s ON s.sucursal_id = ip.sucursal_id
        LEFT JOIN usuarios uc ON uc.usuario_id = ip.usuario_creacion
        LEFT JOIN conceptos c_rol ON c_rol.grupo_id = 1 AND c_rol.codigo = uc.rol_id AND c_rol.marca_baja = false
        LEFT JOIN usuarios ur ON ur.usuario_id = ip.usuario_revision
        LEFT JOIN proveedores pr ON pr.proveedor_id = ip.proveedor_id
        WHERE (:sucursalId IS NULL OR ip.sucursal_id = :sucursalId)
          AND ip.marca_baja = false
        ORDER BY ip.inventario_pendiente_id DESC
    """, nativeQuery = true)
    List<InventarioPendienteDetalleProjection> findAllConDetalleLote(@Param("sucursalId") Integer sucursalId);

    default List<InventarioPendienteDetalleProjection> findAllConDetalleLote() {
        return findAllConDetalleLote(null);
    }
}
