package com.almacen.warnes.modulo_seguridad.repository;

import com.almacen.warnes.modulo_seguridad.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByMarcaBajaFalse();

    Optional<Usuario> findByUsuarioAndMarcaBajaFalse(String usuario);

    boolean existsByUsuarioAndMarcaBajaFalse(String usuario);

    // Llamada a la funcion fn_usuarios_listar de PostgreSQL
    @Query(value = "SELECT * FROM fn_usuarios_listar(:mostrarBajas, :rolId, :sucursalId, :textoBusqueda)", nativeQuery = true)
    List<Object[]> listarUsuariosFiltrados(
            @Param("mostrarBajas") Boolean mostrarBajas,
            @Param("rolId") Integer rolId,
            @Param("sucursalId") Integer sucursalId,
            @Param("textoBusqueda") String textoBusqueda
    );

    // Procedure para modificar usuario
    @Procedure(procedureName = "sp_usuarios_modificar")
    void spUsuariosModificar(
            @Param("p_usuario_id") Integer usuarioId,
            @Param("p_sucursal_id") Integer sucursalId,
            @Param("p_nombres") String nombres,
            @Param("p_apellidos") String apellidos,
            @Param("p_rol_id") Integer rolId,
            @Param("p_usuario_modificacion") Integer usuarioModificacion
    );

    // Procedure para eliminar logicamente
    @Procedure(procedureName = "sp_usuarios_eliminar")
    void spUsuariosEliminar(
            @Param("p_usuario_id") Integer usuarioId,
            @Param("p_usuario_modificacion") Integer usuarioModificacion
    );
}
