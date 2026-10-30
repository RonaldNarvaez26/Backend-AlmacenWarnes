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

    @Query(value = "SELECT * FROM spProveedorKpiResumen()", nativeQuery = true)
    ProveedorKpiProjection obtenerKpiResumen();

    @Query(value = "SELECT * FROM spProveedorListar(:busqueda, :producto)", nativeQuery = true)
    List<ProveedorListadoProjection> listarProveedores(
            @Param("busqueda") String busqueda,
            @Param("producto") String producto
    );

    @Query(value = "SELECT spProveedorRegistrar(:nombreEmpresa, :nombreContacto, :numeroContacto, :anotaciones, :usuarioCreacion)", nativeQuery = true)
    Integer registrarProveedor(
            @Param("nombreEmpresa") String nombreEmpresa,
            @Param("nombreContacto") String nombreContacto,
            @Param("numeroContacto") String numeroContacto,
            @Param("anotaciones") String anotaciones,
            @Param("usuarioCreacion") Integer usuarioCreacion
    );

    @Query(value = "SELECT * FROM spProveedorCatalogoProductos(:proveedorId)", nativeQuery = true)
    List<ProveedorCatalogoProductoProjection> listarCatalogoProductos(@Param("proveedorId") Integer proveedorId);

    @Query(value = "SELECT * FROM spProveedorHistorialLotes(:proveedorId, :filtro)", nativeQuery = true)
    List<ProveedorHistorialLoteProjection> listarHistorialLotes(
            @Param("proveedorId") Integer proveedorId,
            @Param("filtro") String filtro
    );
}
