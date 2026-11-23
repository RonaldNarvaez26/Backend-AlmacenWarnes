package com.almacen.warnes.modulo_analitica.libro_mayor.repository;

import com.almacen.warnes.modulo_analitica.libro_mayor.dto.FiltrosOpcionesDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorItemDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorResumenDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.MovimientoExtraVentaRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LibroMayorRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LibroMayorItemDTO> ITEM_ROW_MAPPER = new RowMapper<LibroMayorItemDTO>() {
        @Override
        public LibroMayorItemDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp ts = rs.getTimestamp("fecha_hora");
            return LibroMayorItemDTO.builder()
                    .referenciaId(rs.getString("referencia_id"))
                    .fechaHora(ts != null ? ts.toLocalDateTime() : null)
                    .origen(rs.getString("origen"))
                    .tipoMovimiento(rs.getInt("tipo_movimiento"))
                    .tipo(rs.getString("tipo"))
                    .metodoMovimiento(rs.getInt("metodo_movimiento"))
                    .metodo(rs.getString("metodo"))
                    .categoriaId(rs.getInt("categoria_id"))
                    .categoria(rs.getString("categoria"))
                    .detalle(rs.getString("detalle"))
                    .monto(rs.getBigDecimal("monto"))
                    .marcaBaja(rs.getBoolean("marca_baja"))
                    .sucursalId(rs.getInt("sucursal_id"))
                    .sucursalNombre(rs.getString("sucursal_nombre"))
                    .cajeroId(rs.getInt("cajero_id"))
                    .cajeroNombre(rs.getString("cajero_nombre"))
                    .productosResumen(rs.getString("productos_resumen"))
                    .build();
        }
    };

    /**
     * Consulta cronológica de movimientos del Libro Mayor con filtros avanzados.
     */
    public List<LibroMayorItemDTO> consultarLibroMayor(
            Integer sucursalId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            Integer cajeroId,
            Integer productoId,
            Integer tipoMovimiento,
            Integer metodoMovimiento,
            Boolean incluirBajas
    ) {
        String sql = "SELECT * FROM fn_libro_mayor_consultar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.query(
                sql,
                ITEM_ROW_MAPPER,
                sucursalId != null && sucursalId > 0 ? sucursalId : null,
                Date.valueOf(fechaInicio),
                Date.valueOf(fechaFin),
                horaInicio != null ? Time.valueOf(horaInicio) : null,
                horaFin != null ? Time.valueOf(horaFin) : null,
                cajeroId != null && cajeroId > 0 ? cajeroId : null,
                productoId != null && productoId > 0 ? productoId : null,
                tipoMovimiento != null && tipoMovimiento > 0 ? tipoMovimiento : null,
                metodoMovimiento != null && metodoMovimiento > 0 ? metodoMovimiento : null,
                incluirBajas != null ? incluirBajas : true
        );
    }

    /**
     * Consulta de totales y resumen financiero del periodo con filtros avanzados.
     */
    public LibroMayorResumenDTO consultarResumen(
            Integer sucursalId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            Integer cajeroId,
            Integer productoId
    ) {
        String sql = "SELECT * FROM fn_libro_mayor_resumen(?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> LibroMayorResumenDTO.builder()
                        .totalVentasQr(rs.getBigDecimal("total_ventas_qr"))
                        .totalVentasEfectivo(rs.getBigDecimal("total_ventas_efectivo"))
                        .totalVentasTarjeta(rs.getBigDecimal("total_ventas_tarjeta"))
                        .totalVentas(rs.getBigDecimal("total_ventas"))
                        .totalEntradasExtra(rs.getBigDecimal("total_entradas_extra"))
                        .totalSalidasExtra(rs.getBigDecimal("total_salidas_extra"))
                        .totalEntradas(rs.getBigDecimal("total_entradas"))
                        .totalSalidas(rs.getBigDecimal("total_salidas"))
                        .totalBajasMonto(rs.getBigDecimal("total_bajas_monto"))
                        .cantidadBajas(rs.getInt("cantidad_bajas"))
                        .saldoNeto(rs.getBigDecimal("saldo_neto"))
                        .build(),
                sucursalId != null && sucursalId > 0 ? sucursalId : null,
                Date.valueOf(fechaInicio),
                Date.valueOf(fechaFin),
                horaInicio != null ? Time.valueOf(horaInicio) : null,
                horaFin != null ? Time.valueOf(horaFin) : null,
                cajeroId != null && cajeroId > 0 ? cajeroId : null,
                productoId != null && productoId > 0 ? productoId : null
        );
    }

    /**
     * Obtiene el JSON del recibo de una venta invocando fn_libro_mayor_recibo_venta.
     */
    public String obtenerReciboVentaJson(Integer ventaId) {
        String sql = "SELECT fn_libro_mayor_recibo_venta(?)";
        return jdbcTemplate.queryForObject(sql, String.class, ventaId);
    }

    /**
     * Obtiene listas de opciones para poblar filtros de Cajero y Producto.
     */
    public FiltrosOpcionesDTO obtenerFiltrosOpciones(Integer sucursalId) {
        String sqlCajeros = "SELECT u.usuario_id, u.usuario, (u.nombres || ' ' || u.apellidos) AS nombre_completo, u.sucursal_id " +
                "FROM usuarios u " +
                "WHERE u.marca_baja = false " +
                (sucursalId != null && sucursalId > 0 ? "AND (u.sucursal_id = ? OR u.sucursal_id IS NULL) " : "") +
                "ORDER BY nombre_completo ASC";

        List<FiltrosOpcionesDTO.CajeroOpcionDTO> cajeros;
        if (sucursalId != null && sucursalId > 0) {
            cajeros = jdbcTemplate.query(sqlCajeros, (rs, rowNum) -> FiltrosOpcionesDTO.CajeroOpcionDTO.builder()
                    .usuarioId(rs.getInt("usuario_id"))
                    .usuario(rs.getString("usuario"))
                    .nombreCompleto(rs.getString("nombre_completo"))
                    .sucursalId(rs.getInt("sucursal_id"))
                    .build(), sucursalId);
        } else {
            cajeros = jdbcTemplate.query(sqlCajeros, (rs, rowNum) -> FiltrosOpcionesDTO.CajeroOpcionDTO.builder()
                    .usuarioId(rs.getInt("usuario_id"))
                    .usuario(rs.getString("usuario"))
                    .nombreCompleto(rs.getString("nombre_completo"))
                    .sucursalId(rs.getInt("sucursal_id"))
                    .build());
        }

        String sqlProductos = "SELECT producto_id, codigo_interno, nombre_producto " +
                "FROM productos " +
                "WHERE marca_baja = false " +
                "ORDER BY nombre_producto ASC";

        List<FiltrosOpcionesDTO.ProductoOpcionDTO> productos = jdbcTemplate.query(sqlProductos, (rs, rowNum) ->
                FiltrosOpcionesDTO.ProductoOpcionDTO.builder()
                        .productoId(rs.getInt("producto_id"))
                        .codigoInterno(rs.getString("codigo_interno"))
                        .nombreProducto(rs.getString("nombre_producto"))
                        .build()
        );

        return FiltrosOpcionesDTO.builder()
                .cajeros(cajeros)
                .productos(productos)
                .build();
    }

    /**
     * Registra un movimiento financiero fuera de venta.
     */
    public Map<String, Object> registrarMovimientoExtra(
            MovimientoExtraVentaRequestDTO dto,
            Integer usuarioId
    ) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_movimiento_extra_venta_registrar(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                cs.setInt(1, dto.getSucursalId());
                cs.setInt(2, dto.getTipoMovimiento());
                if (dto.getCategoriaId() != null) {
                    cs.setInt(3, dto.getCategoriaId());
                } else {
                    cs.setInt(3, dto.getTipoMovimiento());
                }
                cs.setInt(4, dto.getMetodoPago());
                cs.setBigDecimal(5, dto.getMonto());
                cs.setString(6, dto.getDetalle() != null ? dto.getDetalle().trim() : "");
                cs.setInt(7, usuarioId);

                cs.registerOutParameter(8, Types.INTEGER);
                cs.registerOutParameter(9, Types.VARCHAR);

                cs.execute();

                int movimientoId = cs.getInt(8);
                String mensaje = cs.getString(9);

                Map<String, Object> res = new HashMap<>();
                res.put("movimientoId", movimientoId);
                res.put("mensaje", mensaje);
                res.put("exito", movimientoId > 0);
                return res;
            }
        });
    }

    /**
     * Anula lógicamente un movimiento o venta.
     */
    public Map<String, Object> anularMovimiento(
            String origen,
            Integer id,
            Integer usuarioId,
            String motivo
    ) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_movimiento_anular(?, ?, ?, ?, ?, ?)")) {
                cs.setString(1, origen != null ? origen.trim().toUpperCase() : "EXTRA_VENTA");
                cs.setInt(2, id);
                cs.setInt(3, usuarioId);
                cs.setString(4, motivo != null ? motivo.trim() : "");

                cs.registerOutParameter(5, Types.BOOLEAN);
                cs.registerOutParameter(6, Types.VARCHAR);

                cs.execute();

                boolean exito = cs.getBoolean(5);
                String mensaje = cs.getString(6);

                Map<String, Object> res = new HashMap<>();
                res.put("exito", exito);
                res.put("mensaje", mensaje);
                return res;
            }
        });
    }
}
