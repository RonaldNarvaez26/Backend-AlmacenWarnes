package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.Concepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptoRepository extends JpaRepository<Concepto, Integer> {
    List<Concepto> findByGrupoIdAndMarcaBajaFalseOrderByCodigoAsc(Integer grupoId);
}

