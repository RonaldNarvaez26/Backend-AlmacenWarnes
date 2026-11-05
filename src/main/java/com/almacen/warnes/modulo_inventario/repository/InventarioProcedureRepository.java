package com.almacen.warnes.modulo_inventario.repository;

import com.almacen.warnes.modulo_inventario.dto.IngresoDirectoRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class InventarioProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Llama al SP de aprobacion inyectando los datos del lote que viajan en el DTO
     * sin alterar el esquema fisico de inventario_pendientes.
     */
    public void ejecutarAprobarPendiente(
            Integer inventarioPendienteId,
            Integer usuarioRevision,
            Integer proveedorId,
            String descripcionLote,
            BigDecimal precioLote,
            LocalDate fechaVencimiento) {

        String sql = "CALL spInventarioPendientesAprobar(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            CallableStatement cs = connection.prepareCall(sql);

            // 1. p_inventario_pendiente_id INT
            if (inventarioPendienteId != null) {
                cs.setInt(1, inventarioPendienteId);
            } else {
                cs.setNull(1, Types.INTEGER);
            }

            // 2. p_usuario_revision INT
            if (usuarioRevision != null) {
                cs.setInt(2, usuarioRevision);
            } else {
                cs.setInt(2, 1);
            }

            // 3. p_proveedor_id INT (opcional)
            if (proveedorId != null) {
                cs.setInt(3, proveedorId);
            } else {
                cs.setNull(3, Types.INTEGER);
            }

            // 4. p_descripcion_lote VARCHAR(100) (opcional)
            if (descripcionLote != null && !descripcionLote.trim().isEmpty()) {
                cs.setString(4, descripcionLote.trim());
            } else {
                cs.setNull(4, Types.VARCHAR);
            }

            // 5. p_precio_lote DECIMAL(8,2) (opcional)
            if (precioLote != null) {
                cs.setBigDecimal(5, precioLote);
            } else {
                cs.setNull(5, Types.NUMERIC);
            }

            // 6. p_fecha_vencimiento DATE (opcional)
            if (fechaVencimiento != null) {
                cs.setDate(6, Date.valueOf(fechaVencimiento));
            } else {
                cs.setNull(6, Types.DATE);
            }

            return cs;
        });
    }

    public void ejecutarIngresoDirectoSupervisor(IngresoDirectoRequestDTO dto) {
        String sql = "CALL spInventarioIngresoDirectoSupervisor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            CallableStatement cs = connection.prepareCall(sql);

            // 1. p_producto_id INT (opcional)
            if (dto.getProductoId() != null) {
                cs.setInt(1, dto.getProductoId());
            } else {
                cs.setNull(1, Types.INTEGER);
            }

            // 2. p_categoria_id INT
            if (dto.getCategoriaId() != null) {
                cs.setInt(2, dto.getCategoriaId());
            } else {
                cs.setNull(2, Types.INTEGER);
            }

            // 3. p_nombre_producto VARCHAR(100)
            if (dto.getNombreProducto() != null) {
                cs.setString(3, dto.getNombreProducto());
            } else {
                cs.setNull(3, Types.VARCHAR);
            }

            // 4. p_codigo_barras VARCHAR(100)
            String codigoBarras = (dto.getCodigoBarras() != null) ? dto.getCodigoBarras().trim() : "";
            cs.setString(4, codigoBarras);

            // 5. p_codigo_interno VARCHAR(10)
            if (dto.getCodigoInterno() != null) {
                cs.setString(5, dto.getCodigoInterno().trim());
            } else {
                cs.setNull(5, Types.VARCHAR);
            }

            // 6. p_proveedor_id INT (opcional)
            if (dto.getProveedorId() != null) {
                cs.setInt(6, dto.getProveedorId());
            } else {
                cs.setNull(6, Types.INTEGER);
            }

            // 7. p_descripcion_lote VARCHAR(100)
            String descripcionLote = (dto.getDescripcionLote() != null && !dto.getDescripcionLote().trim().isEmpty())
                    ? dto.getDescripcionLote().trim()
                    : "Lote " + (dto.getCodigoInterno() != null ? dto.getCodigoInterno() : "Ingreso");
            cs.setString(7, descripcionLote);

            // 8. p_stock_ingresado INT
            if (dto.getStockIngresado() != null) {
                cs.setInt(8, dto.getStockIngresado());
            } else {
                cs.setNull(8, Types.INTEGER);
            }

            // 9. p_precio_lote DECIMAL(8,2)
            BigDecimal precioLote = dto.getPrecioLote();
            if (precioLote == null && dto.getPrecioCompra() != null && dto.getStockIngresado() != null) {
                precioLote = dto.getPrecioCompra().multiply(BigDecimal.valueOf(dto.getStockIngresado()));
            }
            if (precioLote != null) {
                cs.setBigDecimal(9, precioLote);
            } else {
                cs.setBigDecimal(9, BigDecimal.ZERO);
            }

            // 10. p_precio_compra DECIMAL(8,2)
            if (dto.getPrecioCompra() != null) {
                cs.setBigDecimal(10, dto.getPrecioCompra());
            } else {
                cs.setNull(10, Types.NUMERIC);
            }

            // 11. p_precio_venta DECIMAL(8,2)
            if (dto.getPrecioVenta() != null) {
                cs.setBigDecimal(11, dto.getPrecioVenta());
            } else {
                cs.setNull(11, Types.NUMERIC);
            }

            // 12. p_fecha_vencimiento DATE (opcional)
            if (dto.getFechaVencimiento() != null) {
                cs.setDate(12, Date.valueOf(dto.getFechaVencimiento()));
            } else {
                cs.setNull(12, Types.DATE);
            }

            // 13. p_usuario_creacion INT
            int usuarioCreacion = (dto.getUsuarioCreacion() != null) ? dto.getUsuarioCreacion() : 1;
            cs.setInt(13, usuarioCreacion);

            return cs;
        });
    }
}
