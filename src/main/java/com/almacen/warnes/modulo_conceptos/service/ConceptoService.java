package com.almacen.warnes.modulo_conceptos.service;

import com.almacen.warnes.modulo_conceptos.dto.ConceptoDTO;
import com.almacen.warnes.modulo_conceptos.dto.CrearConceptoDTO;
import com.almacen.warnes.modulo_conceptos.dto.ModificarConceptoDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptoService {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityContextHelper securityContextHelper;

    // Grupos estructurales que no deben ser manipulados por cajeros o supervisores
    private static final List<Integer> GRUPOS_ESTRUCTURALES = List.of(1, 3, 5, 6, 7, 8);

    /**
     * Lista los conceptos activos de un grupo excluyendo la cabecera (código 0)
     * invocando la función PostgreSQL fn_conceptos_listar_por_grupo(p_grupo_id)
     */
    public List<ConceptoDTO> listarPorGrupo(Integer grupoId) {
        String sql = "SELECT concepto_id, codigo, descripcion FROM fn_conceptos_listar_por_grupo(?)";
        return jdbcTemplate.query(sql, (rs, rowNum) -> ConceptoDTO.builder()
                .conceptoId(rs.getInt("concepto_id"))
                .grupoId(grupoId)
                .codigo(rs.getInt("codigo"))
                .descripcion(rs.getString("descripcion"))
                .build(),
                grupoId);
    }

    /**
     * Registra un nuevo concepto paramétrico invocando sp_conceptos_insertar.
     * Valida permisos RBAC:
     * - Grupos estructurales (1, 3, 5, 6, 7, 8): Solo Propietario (rol_id = 3).
     * - Grupo 4 (Métodos de Pago): Solo Supervisor o Propietario.
     * - Grupo 2 (Categorías): Cajero, Supervisor o Propietario.
     */
    @Transactional
    public Integer insertarConcepto(CrearConceptoDTO dto, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = securityContextHelper.obtenerUsuarioSesion(
                dto.getUsuarioCreacion() != null ? dto.getUsuarioCreacion() : usuarioIdParam, authHeader
        ).orElse(null);

        Integer rolId = usuario != null ? usuario.getRolId() : 1;
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (dto.getUsuarioCreacion() != null ? dto.getUsuarioCreacion() : 1);

        validarPermisosGrupoParaCreacion(dto.getGrupoId(), rolId, usuario != null ? usuario.getUsuario() : "Usuario #" + usuarioId);

        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_conceptos_insertar(?, ?, ?, ?)")) {
                cs.setInt(1, dto.getGrupoId());
                cs.setString(2, dto.getDescripcion().trim());
                cs.setInt(3, usuarioId);
                cs.registerOutParameter(4, Types.INTEGER);
                cs.execute();
                return cs.getInt(4);
            }
        });
    }

    /**
     * Modifica la descripción de un concepto existente invocando sp_conceptos_modificar.
     */
    @Transactional
    public void modificarConcepto(Integer conceptoId, ModificarConceptoDTO dto, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = securityContextHelper.obtenerUsuarioSesion(
                dto.getUsuarioModificacion() != null ? dto.getUsuarioModificacion() : usuarioIdParam, authHeader
        ).orElse(null);

        Integer rolId = usuario != null ? usuario.getRolId() : 1;
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (dto.getUsuarioModificacion() != null ? dto.getUsuarioModificacion() : 1);

        Integer grupoId = obtenerGrupoIdDeConcepto(conceptoId);

        validarPermisosGrupoParaModificacion(grupoId, rolId, usuario != null ? usuario.getUsuario() : "Usuario #" + usuarioId);

        jdbcTemplate.update("CALL sp_conceptos_modificar(?, ?, ?)",
                conceptoId, dto.getDescripcion().trim(), usuarioId);
    }

    /**
     * Da de baja lógica a un concepto invocando sp_conceptos_eliminar.
     */
    @Transactional
    public void eliminarConcepto(Integer conceptoId, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader).orElse(null);
        Integer rolId = usuario != null ? usuario.getRolId() : 1;
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (usuarioIdParam != null ? usuarioIdParam : 1);

        Integer grupoId = obtenerGrupoIdDeConcepto(conceptoId);

        // Los grupos estructurales JAMÁS se pueden eliminar
        if (GRUPOS_ESTRUCTURALES.contains(grupoId)) {
            throw new IllegalArgumentException("Acceso denegado: Los conceptos estructurales del sistema (grupo " + grupoId + ") no pueden ser eliminados.");
        }

        if (grupoId == 4 && rolId == 1) {
            throw new IllegalArgumentException("Acceso denegado: Solo el Supervisor o Propietario pueden dar de baja métodos de pago.");
        }

        jdbcTemplate.update("CALL sp_conceptos_eliminar(?, ?)", conceptoId, usuarioId);
    }

    private Integer obtenerGrupoIdDeConcepto(Integer conceptoId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT grupo_id FROM Conceptos WHERE concepto_id = ? AND marca_baja = false",
                    Integer.class, conceptoId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Concepto no encontrado o dado de baja (ID: " + conceptoId + ")");
        }
    }

    private void validarPermisosGrupoParaCreacion(Integer grupoId, Integer rolId, String username) {
        if (GRUPOS_ESTRUCTURALES.contains(grupoId)) {
            if (rolId != 3) {
                log.warn("RBAC: Intento no autorizado del usuario '{}' (Rol: {}) de registrar concepto en grupo estructural {}",
                        username, rolId, grupoId);
                throw new IllegalArgumentException("Acceso denegado: El grupo " + grupoId +
                        " es estructural del sistema (roles/estados) y solo puede ser gestionado por el Propietario.");
            }
        }

        if (grupoId == 4 && rolId == 1) {
            log.warn("RBAC: Cajero '{}' intentando crear método de pago", username);
            throw new IllegalArgumentException("Acceso denegado: Solo el Supervisor o Propietario pueden agregar métodos de pago.");
        }
    }

    private void validarPermisosGrupoParaModificacion(Integer grupoId, Integer rolId, String username) {
        if (GRUPOS_ESTRUCTURALES.contains(grupoId)) {
            if (rolId != 3) {
                log.warn("RBAC: Intento no autorizado del usuario '{}' (Rol: {}) de modificar concepto en grupo estructural {}",
                        username, rolId, grupoId);
                throw new IllegalArgumentException("Acceso denegado: El grupo " + grupoId +
                        " es estructural del sistema y no puede ser modificado por este rol.");
            }
        }

        if (grupoId == 4 && rolId == 1) {
            log.warn("RBAC: Cajero '{}' intentando modificar método de pago", username);
            throw new IllegalArgumentException("Acceso denegado: Solo el Supervisor o Propietario pueden modificar métodos de pago.");
        }
    }
}

