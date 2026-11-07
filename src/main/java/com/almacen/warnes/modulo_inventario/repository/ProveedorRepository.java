package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorCatalogoProductoProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorHistorialLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorListadoProjection;
import com.almacen.warnes.modulo_inventario.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findByMarcaBajaFalse();

    List<Proveedor> findByMarcaBajaFalseOrderByNombreEmpresaAsc();

    List<Proveedor> findBySucursalIdAndMarcaBajaFalseOrderByNombreEmpresaAsc(Integer sucursalId);

    @Query(value = "SELECT * FROM spProveedorKpiResumen(:sucursalId)", nativeQuery = true)
    ProveedorKpiProjection obtenerKpiResumen(@Param("sucursalId") Integer sucursalId);

    @Query(value = "SELECT * FROM spProveedorListar(:busqueda, :producto, :estado, :sucursalId)", nativeQuery = true)
    List<ProveedorListadoProjection> listarProveedores(
            @Param("busqueda") String busqueda,
            @Param("producto") String producto,
            @Param("estado") String estado,
            @Param("sucursalId") Integer sucursalId
    );

    @Query(value = "SELECT spProveedorRegistrar(:nombreEmpresa, :nombreContacto, :numeroContacto, :anotaciones, :usuarioCreacion, :sucursalId)", nativeQuery = true)
    Integer registrarProveedor(
            @Param("nombreEmpresa") String nombreEmpresa,
            @Param("nombreContacto") String nombreContacto,
            @Param("numeroContacto") String numeroContacto,
            @Param("anotaciones") String anotaciones,
            @Param("usuarioCreacion") Integer usuarioCreacion,
            @Param("sucursalId") Integer sucursalId
    );

    @Query(value = "SELECT * FROM spProveedorCatalogoProductos(:proveedorId)", nativeQuery = true)
    List<ProveedorCatalogoProductoProjection> listarCatalogoProductos(@Param("proveedorId") Integer proveedorId);

    @Query(value = "SELECT * FROM spProveedorHistorialLotes(:proveedorId, :filtro)", nativeQuery = true)
    List<ProveedorHistorialLoteProjection> listarHistorialLotes(
            @Param("proveedorId") Integer proveedorId,
            @Param("filtro") String filtro
    );
}
