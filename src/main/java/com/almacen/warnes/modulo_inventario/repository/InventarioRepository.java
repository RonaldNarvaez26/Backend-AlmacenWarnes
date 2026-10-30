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

    @Query(value = "SELECT * FROM spInventarioKpiResumen()", nativeQuery = true)
    InventarioKpiProjection obtenerKpiResumen();

    @Query(value = "SELECT * FROM spInventarioLotesListar()", nativeQuery = true)
    List<InventarioLoteProjection> listarLotes();
}

