package com.almacen.warnes.modulo_ventas.repository;

import com.almacen.warnes.modulo_ventas.dto.ActualizacionStockDTO;
import com.almacen.warnes.modulo_ventas.dto.ClienteDTO;
import com.almacen.warnes.modulo_ventas.dto.DetalleVentaDTO;
import com.almacen.warnes.modulo_ventas.dto.RegistrarVentaDTO;
import com.almacen.warnes.modulo_ventas.dto.VentaResponseDTO;
import com.almacen.warnes.modulo_ventas.mapper.VentaMapper;
import com.almacen.warnes.modulo_ventas.model.Cliente;
import com.almacen.warnes.modulo_ventas.model.DetalleVenta;
import com.almacen.warnes.modulo_ventas.model.ProductoPOS;
import com.almacen.warnes.modulo_ventas.model.Venta;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio del módulo de ventas.
 * Encapsula las llamadas a:
 *   - fn_pos_buscar_producto (FUNCTION)
 *   - sp_ventas_registrar    (PROCEDURE INOUT → retorna nueva ventaId)
 *   - sp_ventas_anular       (PROCEDURE)
 *   - Queries directas a Ventas, Detalles_Ventas, Clientes
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VentaRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final VentaMapper ventaMapper;

    // =========================================================================
    // BUSCADOR POS — fn_pos_buscar_producto
    // =========================================================================

    /**
     * Invoca fn_pos_buscar_producto(p_sucursal_id, p_criterio).
     * Busca por código de barras (exacto), código interno (exacto) o nombre (ILIKE).
     * Prioriza lotes con stock > 0, ordenados por PEPS (fecha_creacion ASC).
     */
    public List<ProductoPOS> buscarProductoPOS(Integer sucursalId, String criterio) {
        String sql = "SELECT inventario_id, producto_id, codigo_barras, codigo_interno, " +
                     "nombre_producto, precio_venta, stock_disponible, en_descuento, precio_descuento " +
                     "FROM fn_pos_buscar_producto(?, ?)";

        return jdbcTemplate.query(sql, (rs, rowNum) -> ProductoPOS.builder()
                .inventarioId(rs.getInt("inventario_id"))
                .productoId(rs.getInt("producto_id"))
                .codigoBarras(rs.getString("codigo_barras"))
                .codigoInterno(rs.getString("codigo_interno"))
                .nombreProducto(rs.getString("nombre_producto"))
                .precioVenta(rs.getBigDecimal("precio_venta"))
                .stockDisponible(rs.getInt("stock_disponible"))
                .enDescuento(rs.getBoolean("en_descuento"))
                .precioDescuento(rs.getBigDecimal("precio_descuento"))
                .build(),
                sucursalId, criterio);
    }

    // =========================================================================
    // REGISTRAR VENTA — sp_ventas_registrar (PROCEDURE INOUT)
    // =========================================================================

    /**
     * Invoca sp_ventas_registrar con todos los parámetros de la venta.
     * Usa INOUT p_nueva_venta_id para obtener el ID de la venta creada.
     * El SP realiza en una sola transacción:
     *   1. Valida que cobros mixtos sumen al total
     *   2. Inserta en Ventas (estado=2 Finalizada)
     *   3. Para cada detalle: bloquea fila (FOR UPDATE), valida stock, descuenta, inserta Detalles_Ventas
     *
     * @param dto         Datos de la venta
     * @param detallesJson JSON con el array de ítems del carrito
     * @return ID de la nueva venta
     */
    public Integer registrarVenta(RegistrarVentaDTO dto, String detallesJson) {
        return jdbcTemplate.execute((Connection conn) -> {
            String call = "CALL sp_ventas_registrar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (CallableStatement cs = conn.prepareCall(call)) {
                cs.setInt(1, dto.getSucursalId());
                cs.setInt(2, dto.getUsuarioId());

                // clienteId — nullable
                if (dto.getClienteId() != null) {
                    cs.setInt(3, dto.getClienteId());
                } else {
                    cs.setNull(3, Types.INTEGER);
                }

                cs.setInt(4, dto.getMetodoVenta());
                cs.setBigDecimal(5, dto.getPrecioTotal());
                cs.setBigDecimal(6, dto.getCobroEfectivo() != null ? dto.getCobroEfectivo() : BigDecimal.ZERO);
                cs.setBigDecimal(7, dto.getCobroQr() != null ? dto.getCobroQr() : BigDecimal.ZERO);
                cs.setBigDecimal(8, dto.getCobroTarjeta() != null ? dto.getCobroTarjeta() : BigDecimal.ZERO);

                // montoRecibido — nullable
                if (dto.getMontoRecibido() != null) {
                    cs.setBigDecimal(9, dto.getMontoRecibido());
                } else {
                    cs.setNull(9, Types.NUMERIC);
                }

                // cambio — nullable
                if (dto.getCambio() != null) {
                    cs.setBigDecimal(10, dto.getCambio());
                } else {
                    cs.setNull(10, Types.NUMERIC);
                }

                // detalles como JSON
                cs.setObject(11, detallesJson, Types.OTHER);

                // INOUT p_nueva_venta_id
                cs.registerOutParameter(12, Types.INTEGER);
                cs.execute();

                return cs.getInt(12);
            }
        });
    }

    // =========================================================================
    // ANULAR VENTA — sp_ventas_anular (PROCEDURE)
    // =========================================================================

    /**
     * Invoca sp_ventas_anular(p_venta_id, p_usuario_anulacion).
     * El SP valida que la venta no esté ya anulada, cambia estado a 3 y revierte el stock.
     */
    public void anularVenta(Integer ventaId, Integer usuarioAnulacion) {
        jdbcTemplate.update("CALL sp_ventas_anular(?, ?)", ventaId, usuarioAnulacion);
    }

    // =========================================================================
    // CONSULTAS DIRECTAS
    // =========================================================================

    /**
     * Obtiene la cabecera de una venta por su ID.
     */
    public Optional<Venta> obtenerVentaPorId(Integer ventaId) {
        String sql = "SELECT venta_id, sucursal_id, estado, precio_total, metodo_venta, " +
                     "cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, " +
                     "cliente_id, anulado_por, marca_baja, fecha_creacion, usuario_creacion, " +
                     "fecha_modificacion, usuario_modificacion " +
                     "FROM Ventas WHERE venta_id = ? AND marca_baja = false";
        List<Venta> result = jdbcTemplate.query(sql, (rs, rowNum) -> mapVenta(rs), ventaId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Obtiene los detalles (líneas de carrito) de una venta junto con el nombre del producto.
     */
    public List<DetalleVenta> obtenerDetallesPorVenta(Integer ventaId) {
        String sql = "SELECT dv.detalle_venta_id, dv.producto_id, dv.venta_id, dv.inventario_id, " +
                     "dv.precio_unitario, dv.cantidad, dv.marca_baja " +
                     "FROM Detalles_Ventas dv " +
                     "WHERE dv.venta_id = ? AND dv.marca_baja = false";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDetalleVenta(rs), ventaId);
    }

    /**
     * Lista ventas invocando la función almacenada sp_ventas_listar(p_sucursal_id, p_fecha, p_metodo_venta, p_estado).
     * Deserializa detalles_json consolidado y genera el reciboHtml para cada venta.
     */
    public List<VentaResponseDTO> listarVentasSP(Integer sucursalId, LocalDate fecha, Integer metodoVenta, Integer estado) {
        String sql = "SELECT venta_id, sucursal_id, sucursal_nombre, estado, estado_nombre, precio_total, " +
                     "metodo_venta, metodo_venta_nombre, cobro_efectivo, cobro_qr, cobro_tarjeta, " +
                     "monto_recibido, cambio, cliente_id, cliente_nombre, cliente_nit, " +
                     "anulado_por, anulado_por_nombre, fecha_creacion, usuario_creacion, cajero_nombre, detalles_json " +
                     "FROM sp_ventas_listar(?, ?, ?, ?)";

        Date sqlDate = fecha != null ? Date.valueOf(fecha) : null;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer ventaId = rs.getInt("venta_id");
            Integer sucId = rs.getInt("sucursal_id");
            String sucursalNombre = rs.getString("sucursal_nombre");
            Integer est = rs.getInt("estado");
            BigDecimal precioTotal = rs.getBigDecimal("precio_total");
            Integer metVenta = rs.getInt("metodo_venta");
            BigDecimal cobroEfectivo = rs.getBigDecimal("cobro_efectivo");
            BigDecimal cobroQr = rs.getBigDecimal("cobro_qr");
            BigDecimal cobroTarjeta = rs.getBigDecimal("cobro_tarjeta");
            BigDecimal montoRecibido = rs.getBigDecimal("monto_recibido");
            BigDecimal cambio = rs.getBigDecimal("cambio");
            Integer clienteId = rs.getObject("cliente_id", Integer.class);
            String clienteNombre = rs.getString("cliente_nombre");
            String clienteNit = rs.getString("cliente_nit");
            Integer anuladoPor = rs.getObject("anulado_por", Integer.class);
            Timestamp fechaCreacionTs = rs.getTimestamp("fecha_creacion");
            LocalDateTime fechaCreacion = fechaCreacionTs != null ? fechaCreacionTs.toLocalDateTime() : null;
            String cajeroNombre = rs.getString("cajero_nombre");

            // Deserializar detalles_json
            String detallesJsonStr = rs.getString("detalles_json");
            List<DetalleVentaDTO> detalles = new ArrayList<>();
            if (detallesJsonStr != null && !detallesJsonStr.isBlank()) {
                try {
                    detalles = objectMapper.readValue(detallesJsonStr, new TypeReference<List<DetalleVentaDTO>>() {});
                } catch (Exception e) {
                    log.warn("Error al deserializar detalles_json para venta #{}: {}", ventaId, e.getMessage());
                }
            }

            // Construir Venta temporal para generar el reciboHtml
            Venta ventaModel = Venta.builder()
                    .ventaId(ventaId)
                    .sucursalId(sucId)
                    .estado(est)
                    .precioTotal(precioTotal)
                    .metodoVenta(metVenta)
                    .cobroEfectivo(cobroEfectivo)
                    .cobroQr(cobroQr)
                    .cobroTarjeta(cobroTarjeta)
                    .montoRecibido(montoRecibido)
                    .cambio(cambio)
                    .clienteId(clienteId)
                    .anuladoPor(anuladoPor)
                    .fechaCreacion(fechaCreacion)
                    .usuarioCreacion(rs.getInt("usuario_creacion"))
                    .build();

            ClienteDTO clienteDTO = null;
            if (clienteId != null) {
                clienteDTO = ClienteDTO.builder()
                        .clienteId(clienteId)
                        .nit(clienteNit)
                        .nombreRazonSocial(clienteNombre)
                        .nombres("")
                        .apellidos("")
                        .telefono("")
                        .build();
            }

            String reciboHtml = ventaMapper.generarReciboHtml(ventaModel, detalles, cajeroNombre, sucursalNombre, clienteDTO);

            return VentaResponseDTO.builder()
                    .ventaId(ventaId)
                    .sucursalId(sucId)
                    .sucursalNombre(sucursalNombre)
                    .estado(est)
                    .precioTotal(precioTotal)
                    .metodoVenta(metVenta)
                    .cobroEfectivo(cobroEfectivo)
                    .cobroQr(cobroQr)
                    .cobroTarjeta(cobroTarjeta)
                    .montoRecibido(montoRecibido)
                    .cambio(cambio)
                    .clienteId(clienteId)
                    .anuladoPor(anuladoPor)
                    .fechaCreacion(fechaCreacion)
                    .cajeroNombre(cajeroNombre)
                    .detalles(detalles)
                    .reciboHtml(reciboHtml)
                    .build();
        }, sucursalId, sqlDate, metodoVenta, estado);
    }

    /**
     * Lista ventas por sucursal con filtro opcional de fecha (yyyy-MM-dd).
     * Retorna solo ventas sin marca_baja.
     */
    public List<Venta> listarVentasPorSucursal(Integer sucursalId, String fecha) {
        StringBuilder sql = new StringBuilder(
                "SELECT venta_id, sucursal_id, estado, precio_total, metodo_venta, " +
                "cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, " +
                "cliente_id, anulado_por, marca_baja, fecha_creacion, usuario_creacion, " +
                "fecha_modificacion, usuario_modificacion " +
                "FROM Ventas WHERE sucursal_id = ? AND marca_baja = false");

        List<Object> params = new ArrayList<>();
        params.add(sucursalId);

        if (fecha != null && !fecha.isBlank()) {
            sql.append(" AND DATE(fecha_creacion) = ?::date");
            params.add(fecha);
        }

        sql.append(" ORDER BY fecha_creacion DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapVenta(rs), params.toArray());
    }

    // =========================================================================
    // CLIENTES
    // =========================================================================

    /**
     * Busca un cliente activo por su NIT/CI.
     */
    public Optional<Cliente> buscarClientePorNit(String nit) {
        String sql = "SELECT cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion, usuario_creacion, fecha_modificacion, usuario_modificacion " +
                     "FROM Clientes WHERE nit = ? AND marca_baja = false LIMIT 1";
        List<Cliente> result = jdbcTemplate.query(sql, (rs, rowNum) -> mapCliente(rs), nit);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Crea un nuevo cliente y retorna el ID generado.
     */
    public Integer crearCliente(ClienteDTO dto, Integer usuarioCreacion) {
        String sql = "INSERT INTO Clientes (nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion, usuario_creacion, fecha_modificacion, usuario_modificacion) " +
                     "VALUES (?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?) " +
                     "RETURNING cliente_id";
        return jdbcTemplate.queryForObject(sql, Integer.class,
                dto.getNit(),
                dto.getNombres(),
                dto.getApellidos(),
                dto.getNombreRazonSocial(),
                dto.getTelefono(),
                usuarioCreacion,
                usuarioCreacion);
    }

    /**
     * Obtiene datos de un cliente por su ID.
     */
    public Optional<Cliente> obtenerClientePorId(Integer clienteId) {
        String sql = "SELECT cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion, usuario_creacion, fecha_modificacion, usuario_modificacion " +
                     "FROM Clientes WHERE cliente_id = ? AND marca_baja = false";
        List<Cliente> result = jdbcTemplate.query(sql, (rs, rowNum) -> mapCliente(rs), clienteId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Consulta el stock actual de una lista de inventario_ids para emitir la notificación WebSocket en tiempo real.
     */
    public List<ActualizacionStockDTO.ItemStockActualizadoDTO> obtenerStockActualizado(List<Integer> inventarioIds) {
        if (inventarioIds == null || inventarioIds.isEmpty()) {
            return Collections.emptyList();
        }
        String inSql = String.join(",", Collections.nCopies(inventarioIds.size(), "?"));
        String sql = "SELECT inventario_id, producto_id, stock FROM Inventario WHERE inventario_id IN (" + inSql + ")";
        return jdbcTemplate.query(sql, (rs, rowNum) -> ActualizacionStockDTO.ItemStockActualizadoDTO.builder()
                .inventarioId(rs.getInt("inventario_id"))
                .productoId(rs.getInt("producto_id"))
                .nuevoStock(rs.getInt("stock"))
                .build(),
                inventarioIds.toArray());
    }

    // =========================================================================
    // ROW MAPPERS PRIVADOS
    // =========================================================================

    private Venta mapVenta(ResultSet rs) throws SQLException {
        return Venta.builder()
                .ventaId(rs.getInt("venta_id"))
                .sucursalId(rs.getInt("sucursal_id"))
                .estado(rs.getInt("estado"))
                .precioTotal(rs.getBigDecimal("precio_total"))
                .metodoVenta(rs.getInt("metodo_venta"))
                .cobroQr(rs.getBigDecimal("cobro_qr"))
                .cobroEfectivo(rs.getBigDecimal("cobro_efectivo"))
                .cobroTarjeta(rs.getBigDecimal("cobro_tarjeta"))
                .montoRecibido(rs.getBigDecimal("monto_recibido"))
                .cambio(rs.getBigDecimal("cambio"))
                .clienteId(rs.getObject("cliente_id", Integer.class))
                .anuladoPor(rs.getObject("anulado_por", Integer.class))
                .marcaBaja(rs.getBoolean("marca_baja"))
                .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime() : null)
                .usuarioCreacion(rs.getInt("usuario_creacion"))
                .fechaModificacion(rs.getTimestamp("fecha_modificacion") != null
                        ? rs.getTimestamp("fecha_modificacion").toLocalDateTime() : null)
                .usuarioModificacion(rs.getInt("usuario_modificacion"))
                .build();
    }

    private DetalleVenta mapDetalleVenta(ResultSet rs) throws SQLException {
        return DetalleVenta.builder()
                .detalleVentaId(rs.getInt("detalle_venta_id"))
                .productoId(rs.getInt("producto_id"))
                .ventaId(rs.getInt("venta_id"))
                .inventarioId(rs.getInt("inventario_id"))
                .precioUnitario(rs.getBigDecimal("precio_unitario"))
                .cantidad(rs.getBigDecimal("cantidad"))
                .marcaBaja(rs.getBoolean("marca_baja"))
                .build();
    }

    private Cliente mapCliente(ResultSet rs) throws SQLException {
        return Cliente.builder()
                .clienteId(rs.getInt("cliente_id"))
                .nit(rs.getString("nit"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .nombreRazonSocial(rs.getString("nombre_razon_social"))
                .telefono(rs.getString("telefono"))
                .marcaBaja(rs.getBoolean("marca_baja"))
                .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime() : null)
                .usuarioCreacion(rs.getInt("usuario_creacion"))
                .fechaModificacion(rs.getTimestamp("fecha_modificacion") != null
                        ? rs.getTimestamp("fecha_modificacion").toLocalDateTime() : null)
                .usuarioModificacion(rs.getInt("usuario_modificacion"))
                .build();
    }
}
