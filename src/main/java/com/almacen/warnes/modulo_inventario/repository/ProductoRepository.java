package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.projection.ProductoCatalogoProjection;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByMarcaBajaFalse();

    Optional<Producto> findByProductoIdAndMarcaBajaFalse(Integer productoId);

    Optional<Producto> findByCodigoInternoAndMarcaBajaFalse(String codigoInterno);

    Optional<Producto> findByCodigoBarrasAndMarcaBajaFalse(String codigoBarras);

    @Query("SELECT p FROM Producto p WHERE p.marcaBaja = false AND (p.codigoBarras = :codigo OR p.codigoInterno = :codigo)")
    Optional<Producto> buscarPorCodigoBarrasOCodigoInterno(@Param("codigo") String codigo);

    @Query(value = "SELECT spProductosGenerarCodigoInterno(:prefijo)", nativeQuery = true)
    String generarCodigoInterno(@Param("prefijo") String prefijo);

    @Query(value = "SELECT * FROM spProductosListarCatalogo()", nativeQuery = true)
    List<ProductoCatalogoProjection> listarCatalogo();
}
