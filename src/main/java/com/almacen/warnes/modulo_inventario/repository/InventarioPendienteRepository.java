package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventarioPendienteRepository extends JpaRepository<InventarioPendiente, Integer> {
    List<InventarioPendiente> findByMarcaBajaFalse();
    List<InventarioPendiente> findByEstadoAndMarcaBajaFalse(Integer estado);
}

