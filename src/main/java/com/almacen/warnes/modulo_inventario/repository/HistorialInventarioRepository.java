package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.projection.HistorialInventarioProjection;
import com.almacen.warnes.modulo_inventario.entity.HistorialInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialInventarioRepository extends JpaRepository<HistorialInventario, Integer> {

    @Query(value = "SELECT * FROM fn_historial_inventario_listar(:sucursalId, :productoId, :tipoMovimiento)", nativeQuery = true)
    List<HistorialInventarioProjection> listarHistorial(
            @Param("sucursalId") Integer sucursalId,
            @Param("productoId") Integer productoId,
            @Param("tipoMovimiento") Integer tipoMovimiento
    );
}

