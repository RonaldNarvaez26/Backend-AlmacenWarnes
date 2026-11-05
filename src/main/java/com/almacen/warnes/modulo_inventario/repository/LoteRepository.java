package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer> {

    List<Lote> findByMarcaBajaFalse();

    @Query("""
        SELECT l FROM Lote l
        JOIN Inventario i ON i.loteId = l.loteId
        WHERE i.productoId = :productoId AND l.marcaBaja = false
        ORDER BY l.loteId DESC
    """)
    List<Lote> findUltimosLotesPorProducto(@Param("productoId") Integer productoId);
}
