package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.projection.InventarioKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioLoteProjection;
import com.almacen.warnes.modulo_inventario.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    List<Inventario> findByMarcaBajaFalse();

    Optional<Inventario> findByInventarioIdAndMarcaBajaFalse(Integer inventarioId);

    Optional<Inventario> findByInventarioIdAndSucursalIdAndMarcaBajaFalse(Integer inventarioId, Integer sucursalId);

    List<Inventario> findByProductoIdAndMarcaBajaFalse(Integer productoId);

    @Query(value = "SELECT * FROM spInventarioKpiResumen(:sucursalId)", nativeQuery = true)
    InventarioKpiProjection obtenerKpiResumen(@org.springframework.data.repository.query.Param("sucursalId") Integer sucursalId);

    default InventarioKpiProjection obtenerKpiResumen() {
        return obtenerKpiResumen(null);
    }

    @Query(value = "SELECT * FROM spInventarioLotesListar(:estado, :sucursalId)", nativeQuery = true)
    List<InventarioLoteProjection> listarLotes(
            @org.springframework.data.repository.query.Param("estado") String estado,
            @org.springframework.data.repository.query.Param("sucursalId") Integer sucursalId);

    default List<InventarioLoteProjection> listarLotes(String estado) {
        return listarLotes(estado, null);
    }
}

