package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.Concepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptoRepository extends JpaRepository<Concepto, Integer> {

    List<Concepto> findByGrupoIdAndMarcaBajaFalseOrderByCodigoAsc(Integer grupoId);

    List<Concepto> findByGrupoIdAndCodigoGreaterThanOrderByCodigoAsc(Integer grupoId, Integer codigo);

    java.util.Optional<Concepto> findByGrupoIdAndCodigo(Integer grupoId, Integer codigo);

    boolean existsByGrupoIdAndDescripcionIgnoreCaseAndMarcaBajaFalse(Integer grupoId, String descripcion);

    @Query("SELECT COALESCE(MAX(c.codigo), 0) + 1 FROM Concepto c WHERE c.grupoId = :grupoId")
    Integer obtenerSiguienteCodigo(@Param("grupoId") Integer grupoId);

    @Query(value = "SELECT nextval('conceptos_concepto_id_seq')", nativeQuery = true)
    Integer obtenerSiguienteConceptoIdSequence();
}
