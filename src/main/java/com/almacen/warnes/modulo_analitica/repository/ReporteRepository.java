package com.almacen.warnes.modulo_analitica.repository;

import com.almacen.warnes.modulo_analitica.dto.ArqueoResumenTurnoDTO;
import com.almacen.warnes.modulo_analitica.dto.ReporteDetalleDTO;
import com.almacen.warnes.modulo_analitica.dto.ReporteListadoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReporteRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Consulta el resumen del turno activo para el cajero vía fn_arqueo_resumen_turno.
     */
    public Optional<ArqueoResumenTurnoDTO> obtenerResumenTurno(Integer usuarioId) {
        String sql = "SELECT sesion_id, sucursal_id, hora_inicio_turno, conteo_ventas, " +
                "total_ventas_efectivo, total_ventas_qr, total_ventas_tarjeta, total_ventas_general, " +
                "total_movimientos_entrada, total_movimientos_salida, cantidad_movimientos, " +
                "requiere_arqueo, movimientos_detalle, top_productos " +
                "FROM fn_arqueo_resumen_turno(?)";

        List<ArqueoResumenTurnoDTO> lista = jdbcTemplate.query(sql, (rs, rowNum) -> ArqueoResumenTurnoDTO.builder()
                .sesionId((Integer) rs.getObject("sesion_id"))
                .sucursalId((Integer) rs.getObject("sucursal_id"))
                .horaInicioTurno(rs.getTimestamp("hora_inicio_turno") != null
                        ? rs.getTimestamp("hora_inicio_turno").toLocalDateTime()
                        : null)
                .conteoVentas(rs.getInt("conteo_ventas"))
                .totalVentasEfectivo(rs.getBigDecimal("total_ventas_efectivo"))
                .totalVentasQr(rs.getBigDecimal("total_ventas_qr"))
                .totalVentasTarjeta(rs.getBigDecimal("total_ventas_tarjeta"))
                .totalVentasGeneral(rs.getBigDecimal("total_ventas_general"))
                .totalMovimientosEntrada(rs.getBigDecimal("total_movimientos_entrada"))
                .totalMovimientosSalida(rs.getBigDecimal("total_movimientos_salida"))
                .cantidadMovimientos(rs.getInt("cantidad_movimientos"))
                .requiereArqueo(rs.getBoolean("requiere_arqueo"))
                .movimientosDetalle(parseJsonArray(rs.getString("movimientos_detalle")))
                .topProductos(parseJsonArray(rs.getString("top_productos")))
                .build(), usuarioId);

        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /**
     * Registra el arqueo de caja de forma atómica invocando sp_reportes_registrar_arqueo.
     */
    public Integer registrarArqueo(
            Integer sesionId,
            Integer usuarioId,
            Integer sucursalId,
            BigDecimal montoBase,
            String snapshotJson,
            String entradasJson,
            String salidasJson,
            String periodo
    ) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_reportes_registrar_arqueo(?, ?, ?, ?, CAST(? AS JSONB), CAST(? AS JSONB), CAST(? AS JSONB), ?, ?)")) {
                cs.setInt(1, sesionId);
                cs.setInt(2, usuarioId);
                cs.setInt(3, sucursalId);

                if (montoBase != null) {
                    cs.setBigDecimal(4, montoBase);
                } else {
                    cs.setNull(4, Types.NUMERIC);
                }

                // Parámetros JSONB mediante casteo explícito SQL
                cs.setString(5, snapshotJson != null ? snapshotJson : "{}");
                cs.setString(6, entradasJson != null ? entradasJson : "[]");
                cs.setString(7, salidasJson != null ? salidasJson : "[]");
                cs.setString(8, periodo != null ? periodo : "Turno");
                cs.registerOutParameter(9, Types.INTEGER);

                cs.execute();
                return cs.getInt(9);
            }
        });
    }

    /**
     * Genera un reporte financiero consolidado por período mediante sp_reportes_generar_periodo.
     */
    public Integer generarReportePeriodo(
            Integer sucursalId,
            String periodo,
            java.time.LocalDateTime fechaInicio,
            java.time.LocalDateTime fechaFin,
            Integer usuarioId
    ) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_reportes_generar_periodo(?, ?, ?, ?, ?, ?)")) {
                cs.setInt(1, sucursalId);
                cs.setString(2, periodo != null ? periodo : "Personalizado");
                cs.setTimestamp(3, java.sql.Timestamp.valueOf(fechaInicio));
                cs.setTimestamp(4, java.sql.Timestamp.valueOf(fechaFin));
                cs.setInt(5, usuarioId);
                cs.registerOutParameter(6, Types.INTEGER);

                cs.execute();
                return cs.getInt(6);
            }
        });
    }

    /**
     * Lista reportes con filtros para el Libro Mayor mediante fn_reportes_listar.
     */
    public List<ReporteListadoDTO> listarReportes(
            Integer sucursalId,
            String periodo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer usuarioId
    ) {
        String sql = "SELECT reporte_id, sucursal_id, codigo_sucursal, nombre_sucursal, periodo, fecha_inicio, fecha_fin, " +
                "monto_base, monto_entradas, monto_salidas, utilidad, datos_snapshot, usuario_creacion, " +
                "nombre_usuario, fecha_creacion " +
                "FROM fn_reportes_listar(CAST(? AS INT), CAST(? AS VARCHAR), CAST(? AS DATE), CAST(? AS DATE), CAST(? AS INT))";

        return jdbcTemplate.query(sql, (rs, rowNum) -> ReporteListadoDTO.builder()
                .reporteId(rs.getInt("reporte_id"))
                .sucursalId(rs.getInt("sucursal_id"))
                .codigoSucursal(rs.getString("codigo_sucursal"))
                .nombreSucursal(rs.getString("nombre_sucursal"))
                .periodo(rs.getString("periodo"))
                .fechaInicio(rs.getTimestamp("fecha_inicio") != null
                        ? rs.getTimestamp("fecha_inicio").toLocalDateTime()
                        : null)
                .fechaFin(rs.getTimestamp("fecha_fin") != null
                        ? rs.getTimestamp("fecha_fin").toLocalDateTime()
                        : null)
                .montoBase(rs.getBigDecimal("monto_base"))
                .montoEntradas(rs.getBigDecimal("monto_entradas"))
                .montoSalidas(rs.getBigDecimal("monto_salidas"))
                .utilidad(rs.getBigDecimal("utilidad"))
                .datosSnapshot(parseJsonObject(rs.getString("datos_snapshot")))
                .usuarioCreacion(rs.getInt("usuario_creacion"))
                .nombreUsuario(rs.getString("nombre_usuario"))
                .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null)
                .build(),
                sucursalId,
                periodo != null && !periodo.isBlank() ? periodo.trim() : null,
                fechaDesde != null ? Date.valueOf(fechaDesde) : null,
                fechaHasta != null ? Date.valueOf(fechaHasta) : null,
                usuarioId
        );
    }

    /**
     * Obtiene el detalle auditado completo de un reporte mediante fn_reportes_obtener_detalle.
     */
    public Optional<ReporteDetalleDTO> obtenerDetalleReporte(Integer reporteId) {
        String sql = "SELECT reporte_id, sucursal_id, codigo_sucursal, nombre_sucursal, periodo, fecha_inicio, fecha_fin, " +
                "monto_base, monto_entradas, monto_salidas, utilidad, datos_snapshot, nombre_usuario, " +
                "fecha_creacion, entradas, salidas, movimientos " +
                "FROM fn_reportes_obtener_detalle(?)";

        List<ReporteDetalleDTO> lista = jdbcTemplate.query(sql, (rs, rowNum) -> ReporteDetalleDTO.builder()
                .reporteId(rs.getInt("reporte_id"))
                .sucursalId(rs.getInt("sucursal_id"))
                .codigoSucursal(rs.getString("codigo_sucursal"))
                .nombreSucursal(rs.getString("nombre_sucursal"))
                .periodo(rs.getString("periodo"))
                .fechaInicio(rs.getTimestamp("fecha_inicio") != null
                        ? rs.getTimestamp("fecha_inicio").toLocalDateTime()
                        : null)
                .fechaFin(rs.getTimestamp("fecha_fin") != null
                        ? rs.getTimestamp("fecha_fin").toLocalDateTime()
                        : null)
                .montoBase(rs.getBigDecimal("monto_base"))
                .montoEntradas(rs.getBigDecimal("monto_entradas"))
                .montoSalidas(rs.getBigDecimal("monto_salidas"))
                .utilidad(rs.getBigDecimal("utilidad"))
                .datosSnapshot(parseJsonObject(rs.getString("datos_snapshot")))
                .nombreUsuario(rs.getString("nombre_usuario"))
                .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null)
                .entradas(parseJsonArray(rs.getString("entradas")))
                .salidas(parseJsonArray(rs.getString("salidas")))
                .movimientos(parseJsonArray(rs.getString("movimientos")))
                .build(), reporteId);

        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private Object parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(jsonStr, Object.class);
        } catch (Exception e) {
            log.warn("Error al deserializar jsonb array en ReporteRepository: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Object parseJsonObject(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(jsonStr, Object.class);
        } catch (Exception e) {
            log.warn("Error al deserializar jsonb object en ReporteRepository: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
