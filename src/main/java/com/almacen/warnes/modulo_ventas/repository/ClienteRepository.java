package com.almacen.warnes.modulo_ventas.repository;

import com.almacen.warnes.modulo_ventas.dto.ClienteDTO;
import com.almacen.warnes.modulo_ventas.model.Cliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio dedicado para la entidad Clientes.
 * Interactúa con la base de datos PostgreSQL mediante Procedimientos Almacenados (SPs).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClienteRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Busca un cliente activo por su NIT/CI exacto usando el SP sp_clientes_buscar_nit.
     */
    public Optional<Cliente> buscarClientePorNit(String nit) {
        String sql = "SELECT cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion FROM sp_clientes_buscar_nit(?)";
        List<Cliente> result = jdbcTemplate.query(sql, (rs, rowNum) -> Cliente.builder()
                .clienteId(rs.getInt("cliente_id"))
                .nit(rs.getString("nit"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .nombreRazonSocial(rs.getString("nombre_razon_social"))
                .telefono(rs.getString("telefono"))
                .marcaBaja(rs.getBoolean("marca_baja"))
                .fechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")))
                .build(), nit);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Obtiene un cliente activo por su ID.
     */
    public Optional<Cliente> obtenerClientePorId(Integer clienteId) {
        String sql = "SELECT cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion, usuario_creacion, fecha_modificacion, usuario_modificacion " +
                     "FROM Clientes WHERE cliente_id = ? AND marca_baja = false";
        List<Cliente> result = jdbcTemplate.query(sql, (rs, rowNum) -> mapCliente(rs), clienteId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Lista clientes con filtros de búsqueda y estado usando sp_clientes_listar.
     */
    public List<ClienteDTO> listarClientes(String busqueda, String estado) {
        String sql = "SELECT cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, " +
                     "marca_baja, fecha_creacion, fecha_modificacion, total_compras, monto_total_comprado " +
                     "FROM sp_clientes_listar(?, ?)";
        return jdbcTemplate.query(sql, (rs, rowNum) -> ClienteDTO.builder()
                .clienteId(rs.getInt("cliente_id"))
                .nit(rs.getString("nit"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .nombreRazonSocial(rs.getString("nombre_razon_social"))
                .telefono(rs.getString("telefono"))
                .marcaBaja(rs.getBoolean("marca_baja"))
                .fechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")))
                .fechaModificacion(toLocalDateTime(rs.getTimestamp("fecha_modificacion")))
                .totalCompras(rs.getLong("total_compras"))
                .montoTotalComprado(rs.getBigDecimal("monto_total_comprado"))
                .build(), busqueda, estado);
    }

    /**
     * Registra o reactiva un cliente invocando sp_clientes_insertar.
     */
    public Integer crearCliente(ClienteDTO dto, Integer usuarioCreacion) {
        String sql = "SELECT sp_clientes_insertar(?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.queryForObject(sql, Integer.class,
                dto.getNit(),
                dto.getNombres(),
                dto.getApellidos(),
                dto.getNombreRazonSocial(),
                dto.getTelefono(),
                usuarioCreacion != null ? usuarioCreacion : 1
        );
    }

    /**
     * Modifica un cliente existente invocando sp_clientes_modificar.
     */
    public void modificarCliente(Integer clienteId, ClienteDTO dto, Integer usuarioModificacion) {
        String sql = "CALL sp_clientes_modificar(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                clienteId,
                dto.getNit(),
                dto.getNombres(),
                dto.getApellidos(),
                dto.getNombreRazonSocial(),
                dto.getTelefono(),
                usuarioModificacion != null ? usuarioModificacion : 1
        );
    }

    /**
     * Da de baja lógica a un cliente invocando sp_clientes_eliminar.
     */
    public void eliminarCliente(Integer clienteId, Integer usuarioModificacion) {
        String sql = "CALL sp_clientes_eliminar(?, ?)";
        jdbcTemplate.update(sql, clienteId, usuarioModificacion != null ? usuarioModificacion : 1);
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
                .fechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")))
                .usuarioCreacion(rs.getObject("usuario_creacion", Integer.class))
                .fechaModificacion(toLocalDateTime(rs.getTimestamp("fecha_modificacion")))
                .usuarioModificacion(rs.getObject("usuario_modificacion", Integer.class))
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
