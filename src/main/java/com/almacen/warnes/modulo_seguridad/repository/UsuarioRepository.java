package com.almacen.warnes.modulo_seguridad.repository;

import com.almacen.warnes.modulo_seguridad.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findByMarcaBajaFalse();
    Optional<Usuario> findByUsuarioAndMarcaBajaFalse(String usuario);
}
