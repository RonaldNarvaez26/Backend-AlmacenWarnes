package com.almacen.warnes.modulo_seguridad.repository;

import com.almacen.warnes.modulo_seguridad.dto.UsuarioConRolProjection;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findByMarcaBajaFalse();
    Optional<Usuario> findByUsuarioAndMarcaBajaFalse(String usuario);

    @Query(value = """
        SELECT u.usuario_id AS usuarioId,
               u.usuario AS usuario,
               u.nombres AS nombres,
               u.apellidos AS apellidos,
               u.rol_id AS rolId,
               COALESCE(c.descripcion, 'Personal') AS rolNombre,
               TRIM(CONCAT(u.nombres, ' ', u.apellidos)) AS nombreCompleto,
               u.marca_baja AS marcaBaja,
               u.fecha_creacion AS fechaCreacion,
               u.usuario_creacion AS usuarioCreacion,
               u.fecha_modificacion AS fechaModificacion,
               u.usuario_modificacion AS usuarioModificacion
        FROM usuarios u
        LEFT JOIN conceptos c ON c.grupo_id = 1 AND c.codigo = u.rol_id
        WHERE u.marca_baja = false
        ORDER BY u.rol_id DESC, u.usuario_id ASC
    """, nativeQuery = true)
    List<UsuarioConRolProjection> listarUsuariosActivosConRol();

    @Query(value = """
        SELECT u.usuario_id AS usuarioId,
               u.usuario AS usuario,
               u.nombres AS nombres,
               u.apellidos AS apellidos,
               u.rol_id AS rolId,
               COALESCE(c.descripcion, 'Personal') AS rolNombre,
               TRIM(CONCAT(u.nombres, ' ', u.apellidos)) AS nombreCompleto,
               u.marca_baja AS marcaBaja,
               u.fecha_creacion AS fechaCreacion,
               u.usuario_creacion AS usuarioCreacion,
               u.fecha_modificacion AS fechaModificacion,
               u.usuario_modificacion AS usuarioModificacion
        FROM usuarios u
        LEFT JOIN conceptos c ON c.grupo_id = 1 AND c.codigo = u.rol_id
        WHERE u.usuario_id = :id
    """, nativeQuery = true)
    Optional<UsuarioConRolProjection> obtenerUsuarioConRolPorId(@Param("id") Integer id);
}
