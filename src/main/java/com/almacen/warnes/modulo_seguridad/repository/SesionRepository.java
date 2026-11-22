package com.almacen.warnes.modulo_seguridad.repository;

import com.almacen.warnes.modulo_seguridad.dto.SesionDTO;
import com.almacen.warnes.modulo_seguridad.dto.SesionListadoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SesionRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Abre una sesión para el usuario o retoma la sesión abierta si aún no fue cerrada.
     */
    public Integer abrirSesion(Integer usuarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_sesiones_abrir(?, ?)")) {
                cs.setInt(1, usuarioId);
                cs.registerOutParameter(2, Types.INTEGER);
                cs.execute();
                return cs.getInt(2);
            }
        });
    }

    /**
     * Cierra la sesión activa del usuario y calcula la duración en minutos.
     */
    public Integer cerrarSesion(Integer sesionId, Integer usuarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_sesiones_cerrar(?, ?, ?)")) {
                cs.setInt(1, sesionId);
                cs.setInt(2, usuarioId);
                cs.registerOutParameter(3, Types.INTEGER);
                cs.execute();
                return cs.getInt(3);
            }
        });
    }

    /**
     * Obtiene la sesión activa de un usuario mediante fn_sesiones_obtener_activa.
     */
    public Optional<SesionDTO> obtenerSesionActiva(Integer usuarioId) {
        String sql = "SELECT sesion_id, usuario_id, hora_sesion_abierta, minutos_transcurridos " +
                "FROM fn_sesiones_obtener_activa(?)";

        List<SesionDTO> lista = jdbcTemplate.query(sql, (rs, rowNum) -> SesionDTO.builder()
                .sesionId(rs.getInt("sesion_id"))
                .usuarioId(rs.getInt("usuario_id"))
                .horaSesionAbierta(rs.getTimestamp("hora_sesion_abierta") != null
                        ? rs.getTimestamp("hora_sesion_abierta").toLocalDateTime()
                        : null)
                .minutosTranscurridos(rs.getInt("minutos_transcurridos"))
                .build(), usuarioId);

        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /**
     * Lista sesiones con filtros para auditoría mediante fn_sesiones_listar.
     */
    public List<SesionListadoDTO> listarSesiones(
            Integer usuarioId,
            Integer sucursalId,
            Boolean soloActivas,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String textoBusqueda
    ) {
        String sql = "SELECT sesion_id, usuario_id, usuario, nombre_completo, descripcion_rol, " +
                "sucursal_id, codigo_sucursal, hora_sesion_abierta, hora_sesion_cerrada, " +
                "duracion_sesion, esta_activa, reporte_id, estado_arqueo, alerta_sin_arqueo " +
                "FROM fn_sesiones_listar(?, ?, ?, ?, ?, ?)";

        return jdbcTemplate.query(sql, (rs, rowNum) -> SesionListadoDTO.builder()
                .sesionId(rs.getInt("sesion_id"))
                .usuarioId(rs.getInt("usuario_id"))
                .usuario(rs.getString("usuario"))
                .nombreCompleto(rs.getString("nombre_completo"))
                .descripcionRol(rs.getString("descripcion_rol"))
                .sucursalId((Integer) rs.getObject("sucursal_id"))
                .codigoSucursal(rs.getString("codigo_sucursal"))
                .horaSesionAbierta(rs.getTimestamp("hora_sesion_abierta") != null
                        ? rs.getTimestamp("hora_sesion_abierta").toLocalDateTime()
                        : null)
                .horaSesionCerrada(rs.getTimestamp("hora_sesion_cerrada") != null
                        ? rs.getTimestamp("hora_sesion_cerrada").toLocalDateTime()
                        : null)
                .duracionSesion((Integer) rs.getObject("duracion_sesion"))
                .estaActiva(rs.getBoolean("esta_activa"))
                .reporteId((Integer) rs.getObject("reporte_id"))
                .estadoArqueo(rs.getString("estado_arqueo"))
                .alertaSinArqueo(rs.getBoolean("alerta_sin_arqueo"))
                .build(),
                usuarioId,
                sucursalId,
                soloActivas,
                fechaDesde != null ? Date.valueOf(fechaDesde) : null,
                fechaHasta != null ? Date.valueOf(fechaHasta) : null,
                textoBusqueda != null && !textoBusqueda.isBlank() ? textoBusqueda.trim() : null
        );
    }

    /**
     * Purga físicamente sesiones cerradas antiguas mediante sp_sesiones_purgar_antiguas.
     */
    public Integer purgarSesionesAntiguas(Integer mesesRetencion) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_sesiones_purgar_antiguas(?, ?)")) {
                cs.setInt(1, mesesRetencion != null ? mesesRetencion : 3);
                cs.registerOutParameter(2, Types.INTEGER);
                cs.execute();
                return cs.getInt(2);
            }
        });
    }
}
