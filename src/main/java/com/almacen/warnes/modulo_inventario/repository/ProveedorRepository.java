package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    List<Proveedor> findByMarcaBajaFalse();
    List<Proveedor> findByMarcaBajaFalseOrderByNombreEmpresaAsc();
}

