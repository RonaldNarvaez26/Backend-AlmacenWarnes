package com.almacen.warnes.modulo_notificaciones.repository;

import com.almacen.warnes.modulo_notificaciones.dto.ConfigVencimientoCategoriaDTO;
import com.almacen.warnes.modulo_notificaciones.dto.NotificacionItemDTO;
import com.almacen.warnes.modulo_notificaciones.dto.VerificacionStockVentaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificacionProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Invoca la función fn_notificaciones_listar(p_usuario_id, p_rol_id, p_sucursal_id)
     */
    public List<NotificacionItemDTO> listarNotificaciones(Integer usuarioId, Integer rolId, Integer sucursalId) {
        String sql = "SELECT id_notificacion, tipo_alerta, severidad, titulo, mensaje, " +
                     "       sucursal_id, codigo_sucursal, referencia_id, ruta_accion, fecha_evento, " +
                     "       leido, fecha_leido, tipo_notificacion " +
                     "FROM fn_notificaciones_listar(?, ?, ?)";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("fecha_evento");
            Timestamp tsLeido = rs.getTimestamp("fecha_leido");

            int sucId = rs.getInt("sucursal_id");
            Integer sucursalIdVal = rs.wasNull() ? null : sucId;

            int refId = rs.getInt("referencia_id");
            Integer referenciaIdVal = rs.wasNull() ? null : refId;

            int tipoNotif = rs.getInt("tipo_notificacion");
            Integer tipoNotifVal = rs.wasNull() ? null : tipoNotif;

            String ruta = rs.getString("ruta_accion");

            return NotificacionItemDTO.builder()
                    .idNotificacion(rs.getString("id_notificacion"))
                    .tipoAlerta(rs.getString("tipo_alerta"))
                    .severidad(rs.getString("severidad"))
                    .titulo(rs.getString("titulo"))
                    .mensaje(rs.getString("mensaje"))
                    .sucursalId(sucursalIdVal)
                    .codigoSucursal(rs.getString("codigo_sucursal"))
                    .referenciaId(referenciaIdVal)
                    .rutaAccion(ruta)
                    .direccion(ruta)
                    .fechaEvento(ts != null ? ts.toLocalDateTime() : null)
                    .leido(rs.getBoolean("leido"))
                    .fechaLeido(tsLeido != null ? tsLeido.toLocalDateTime() : null)
                    .tipoNotificacion(tipoNotifVal)
                    .build();
        }, usuarioId, rolId, sucursalId);
    }

    /**
     * Invoca el procedimiento sp_notificaciones_marcar_leido(p_id_notificacion, p_usuario_id, out p_resultado)
     */
    public boolean marcarNotificacionLeida(Integer idNotificacion, Integer usuarioId) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_marcar_leido(?, ?, ?)")) {
                cs.setInt(1, idNotificacion);
                cs.setInt(2, usuarioId != null ? usuarioId : 0);
                cs.registerOutParameter(3, Types.BOOLEAN);
                cs.execute();
                return cs.getBoolean(3);
            }
        }));
    }

    /**
     * Invoca el procedimiento sp_notificaciones_marcar_todas_leidas(p_usuario_id, p_rol_id, out p_afectados)
     */
    public int marcarTodasNotificacionesLeidas(Integer usuarioId, Integer rolId) {
        Integer afectados = jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_marcar_todas_leidas(?, ?, ?)")) {
                cs.setInt(1, usuarioId != null ? usuarioId : 0);
                cs.setInt(2, rolId != null ? rolId : 0);
                cs.registerOutParameter(3, Types.INTEGER);
                cs.execute();
                return cs.getInt(3);
            }
        });
        return afectados != null ? afectados : 0;
    }

    /**
     * Invoca la función fn_notificaciones_verificar_stock_venta(p_sucursal_id, p_producto_id, p_cantidad_solicitada)
     */
    public VerificacionStockVentaDTO verificarStockVenta(Integer sucursalId, Integer productoId, BigDecimal cantidadSolicitada) {
        String sql = "SELECT puede_vender, stock_actual, stock_restante, nivel_alerta, mensaje_alerta " +
                     "FROM fn_notificaciones_verificar_stock_venta(?, ?, ?)";

        List<VerificacionStockVentaDTO> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            return VerificacionStockVentaDTO.builder()
                    .puedeVender(rs.getBoolean("puede_vender"))
                    .stockActual(rs.getInt("stock_actual"))
                    .stockRestante(rs.getBigDecimal("stock_restante"))
                    .nivelAlerta(rs.getString("nivel_alerta"))
                    .mensajeAlerta(rs.getString("mensaje_alerta"))
                    .build();
        }, sucursalId, productoId, cantidadSolicitada);

        if (list.isEmpty()) {
            return VerificacionStockVentaDTO.builder()
                    .puedeVender(false)
                    .stockActual(0)
                    .stockRestante(BigDecimal.ZERO)
                    .nivelAlerta("ERROR")
                    .mensajeAlerta("No fue posible verificar la disponibilidad de stock.")
                    .build();
        }
        return list.get(0);
    }

    /**
     * Invoca el procedimiento sp_notificaciones_solicitar_dashboard(p_usuario_id, out estado, out mensaje)
     */
    public Map<String, String> solicitarDashboard(Integer usuarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_solicitar_dashboard(?, ?, ?)")) {
                cs.setInt(1, usuarioId);
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.execute();

                Map<String, String> res = new HashMap<>();
                res.put("estado", cs.getString(2));
                res.put("mensaje", cs.getString(3));
                return res;
            }
        });
    }

    /**
     * Invoca el procedimiento sp_notificaciones_resolver_dashboard(p_supervisor_id, p_autorizar, p_propietario_id, out estado, out mensaje)
     */
    public Map<String, String> resolverDashboard(Integer supervisorId, Boolean autorizar, Integer propietarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_resolver_dashboard(?, ?, ?, ?, ?)")) {
                cs.setInt(1, supervisorId);
                cs.setBoolean(2, autorizar != null && autorizar);
                cs.setInt(3, propietarioId);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();

                Map<String, String> res = new HashMap<>();
                res.put("estado", cs.getString(4));
                res.put("mensaje", cs.getString(5));
                return res;
            }
        });
    }

    /**
     * Consulta el estado actual de la solicitud en la tabla conceptos (Grupo 10)
     */
    public String obtenerEstadoDashboardSupervisor(Integer supervisorId) {
        String sql = "SELECT descripcion FROM conceptos " +
                     "WHERE grupo_id = 10 AND codigo = ? AND marca_baja = FALSE " +
                     "ORDER BY concepto_id DESC LIMIT 1";

        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("descripcion"), supervisorId);
        return list.isEmpty() ? "NINGUNO" : list.get(0).trim().toUpperCase();
    }

    /**
     * Invoca la función fn_notificaciones_config_vencimiento_listar()
     */
    public List<ConfigVencimientoCategoriaDTO> listarConfiguracionVencimiento() {
        String sql = "SELECT categoria_id, nombre_categoria, dias_anticipacion, lotes_afectados " +
                     "FROM fn_notificaciones_config_vencimiento_listar()";

        return jdbcTemplate.query(sql, (rs, rowNum) -> ConfigVencimientoCategoriaDTO.builder()
                .categoriaId(rs.getInt("categoria_id"))
                .nombreCategoria(rs.getString("nombre_categoria"))
                .diasAnticipacion(rs.getInt("dias_anticipacion"))
                .lotesAfectados(rs.getInt("lotes_afectados"))
                .build());
    }

    /**
     * Invoca el procedimiento sp_notificaciones_config_vencimiento_global(p_dias_anticipacion, p_usuario_id, out p_afectados, out p_mensaje)
     */
    public Map<String, Object> actualizarVencimientoGlobal(int diasAnticipacion, Integer usuarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_config_vencimiento_global(?, ?, ?, ?)")) {
                cs.setInt(1, diasAnticipacion);
                cs.setInt(2, usuarioId != null ? usuarioId : 1);
                cs.registerOutParameter(3, Types.INTEGER);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();

                Map<String, Object> res = new HashMap<>();
                res.put("afectados", cs.getInt(3));
                res.put("mensaje", cs.getString(4));
                res.put("diasAnticipacion", diasAnticipacion);
                return res;
            }
        });
    }

    /**
     * Invoca el procedimiento sp_notificaciones_config_vencimiento_individual(p_categoria_id, p_dias_anticipacion, p_usuario_id, out p_resultado, out p_mensaje)
     */
    public Map<String, Object> actualizarVencimientoIndividual(int categoriaId, int diasAnticipacion, Integer usuarioId) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_notificaciones_config_vencimiento_individual(?, ?, ?, ?, ?)")) {
                cs.setInt(1, categoriaId);
                cs.setInt(2, diasAnticipacion);
                cs.setInt(3, usuarioId != null ? usuarioId : 1);
                cs.registerOutParameter(4, Types.BOOLEAN);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();

                Map<String, Object> res = new HashMap<>();
                res.put("resultado", cs.getBoolean(4));
                res.put("mensaje", cs.getString(5));
                res.put("categoriaId", categoriaId);
                res.put("diasAnticipacion", diasAnticipacion);
                return res;
            }
        });
    }
}
