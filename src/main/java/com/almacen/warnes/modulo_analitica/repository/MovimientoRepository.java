package com.almacen.warnes.modulo_analitica.repository;

import com.almacen.warnes.modulo_analitica.dto.MovimientoListadoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MovimientoRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Inserta un nuevo movimiento invocando sp_movimientos_insertar.
     */
    public Integer insertarMovimiento(
            Integer sucursalId,
            BigDecimal monto,
            Integer tipoMovimiento,
            Integer metodoMovimiento,
            String detalle,
            Integer usuarioCreacion
    ) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_movimientos_insertar(?, ?, ?, ?, ?, ?, ?)")) {
                cs.setInt(1, sucursalId);
                cs.setBigDecimal(2, monto);
                cs.setInt(3, tipoMovimiento);
                cs.setInt(4, metodoMovimiento);
                cs.setString(5, detalle != null ? detalle.trim() : "");
                cs.setInt(6, usuarioCreacion);
                cs.registerOutParameter(7, Types.INTEGER);
                cs.execute();
                return cs.getInt(7);
            }
        });
    }

    /**
     * Modifica un movimiento pendiente invocando sp_movimientos_modificar.
     */
    public void modificarMovimiento(
            Integer movimientoId,
            BigDecimal monto,
            Integer tipoMovimiento,
            Integer metodoMovimiento,
            String detalle,
            Integer usuarioModificacion
    ) {
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_movimientos_modificar(?, ?, ?, ?, ?, ?)")) {
                cs.setInt(1, movimientoId);
                if (monto != null) cs.setBigDecimal(2, monto); else cs.setNull(2, Types.NUMERIC);
                if (tipoMovimiento != null) cs.setInt(3, tipoMovimiento); else cs.setNull(3, Types.INTEGER);
                if (metodoMovimiento != null) cs.setInt(4, metodoMovimiento); else cs.setNull(4, Types.INTEGER);
                if (detalle != null) cs.setString(5, detalle.trim()); else cs.setNull(5, Types.VARCHAR);
                cs.setInt(6, usuarioModificacion);
                cs.execute();
                return null;
            }
        });
    }

    /**
     * Da de baja lógica un movimiento pendiente invocando sp_movimientos_eliminar.
     */
    public void eliminarMovimiento(Integer movimientoId, Integer usuarioModificacion) {
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_movimientos_eliminar(?, ?)")) {
                cs.setInt(1, movimientoId);
                cs.setInt(2, usuarioModificacion);
                cs.execute();
                return null;
            }
        });
    }

    /**
     * Lista movimientos con filtros invocando fn_movimientos_listar.
     */
    public List<MovimientoListadoDTO> listarMovimientos(
            Integer sucursalId,
            Integer reporteId,
            Boolean soloPendientes,
            Integer tipoMovimiento,
            Integer usuarioId,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {
        String sql = "SELECT movimiento_id, reporte_id, monto, tipo_movimiento, descripcion_tipo, " +
                "metodo_movimiento, descripcion_metodo, detalle, usuario_creacion, nombre_usuario, " +
                "sucursal_id, fecha_creacion " +
                "FROM fn_movimientos_listar(?, ?, ?, ?, ?, ?, ?)";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer repId = (Integer) rs.getObject("reporte_id");
            return MovimientoListadoDTO.builder()
                    .movimientoId(rs.getInt("movimiento_id"))
                    .reporteId(repId)
                    .monto(rs.getBigDecimal("monto"))
                    .tipoMovimiento(rs.getInt("tipo_movimiento"))
                    .descripcionTipo(rs.getString("descripcion_tipo"))
                    .metodoMovimiento(rs.getInt("metodo_movimiento"))
                    .descripcionMetodo(rs.getString("descripcion_metodo"))
                    .detalle(rs.getString("detalle"))
                    .usuarioCreacion(rs.getInt("usuario_creacion"))
                    .nombreUsuario(rs.getString("nombre_usuario"))
                    .sucursalId(rs.getInt("sucursal_id"))
                    .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                            ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                            : null)
                    .sellado(repId != null)
                    .build();
        },
                sucursalId,
                reporteId,
                soloPendientes != null ? soloPendientes : false,
                tipoMovimiento,
                usuarioId,
                fechaDesde != null ? Date.valueOf(fechaDesde) : null,
                fechaHasta != null ? Date.valueOf(fechaHasta) : null
        );
    }
}
