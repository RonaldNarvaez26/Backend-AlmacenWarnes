package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer> {
    List<Lote> findByMarcaBajaFalse();
}

