package com.almacen.warnes.modulo_seguridad.repository;

import com.almacen.warnes.modulo_seguridad.dto.CrearSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.ModificarSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.SucursalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SucursalProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Invoca la función almacenada fn_sucursales_listar(p_mostrar_bajas, p_texto_busqueda)
     */
    public List<SucursalDTO> listarSucursales(Boolean mostrarBajas, String textoBusqueda) {
        String sql = "SELECT sucursal_id, codigo_interno, ciudad, direccion, telefono, fecha_creacion, cantidad_usuarios " +
                     "FROM fn_sucursales_listar(?, ?)";

        String busquedaParam = (textoBusqueda != null && !textoBusqueda.trim().isEmpty())
                ? textoBusqueda.trim()
                : null;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("fecha_creacion");
            return SucursalDTO.builder()
                    .sucursalId(rs.getInt("sucursal_id"))
                    .codigoInterno(rs.getString("codigo_interno"))
                    .ciudad(rs.getString("ciudad"))
                    .direccion(rs.getString("direccion"))
                    .telefono(rs.getString("telefono"))
                    .fechaCreacion(ts != null ? ts.toLocalDateTime() : null)
                    .cantidadUsuarios(rs.getInt("cantidad_usuarios"))
                    .marcaBaja(false)
                    .build();
        }, mostrarBajas != null ? mostrarBajas : false, busquedaParam);
    }

    /**
     * Invoca el procedimiento sp_sucursales_insertar
     */
    public Integer insertarSucursal(CrearSucursalDTO dto, Integer usuarioCreacion) {
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_sucursales_insertar(?, ?, ?, ?, ?, ?)")) {
                cs.setString(1, dto.getCodigoInterno() != null ? dto.getCodigoInterno().trim() : null);
                cs.setString(2, dto.getCiudad() != null ? dto.getCiudad().trim() : null);
                cs.setString(3, dto.getDireccion() != null ? dto.getDireccion().trim() : null);
                cs.setString(4, dto.getTelefono() != null ? dto.getTelefono().trim() : null);
                cs.setInt(5, usuarioCreacion != null ? usuarioCreacion : 1);
                cs.registerOutParameter(6, Types.INTEGER);
                cs.execute();
                return cs.getInt(6);
            }
        });
    }

    /**
     * Invoca el procedimiento sp_sucursales_modificar
     */
    public void modificarSucursal(Integer sucursalId, ModificarSucursalDTO dto, Integer usuarioModificacion) {
        jdbcTemplate.update("CALL sp_sucursales_modificar(?, ?, ?, ?, ?, ?)",
                sucursalId,
                dto.getCodigoInterno() != null ? dto.getCodigoInterno().trim() : null,
                dto.getCiudad() != null ? dto.getCiudad().trim() : null,
                dto.getDireccion() != null ? dto.getDireccion().trim() : null,
                dto.getTelefono() != null ? dto.getTelefono().trim() : null,
                usuarioModificacion != null ? usuarioModificacion : 1);
    }

    /**
     * Invoca el procedimiento sp_sucursales_eliminar (baja lógica)
     */
    public void eliminarSucursal(Integer sucursalId, Integer usuarioModificacion) {
        jdbcTemplate.update("CALL sp_sucursales_eliminar(?, ?)",
                sucursalId,
                usuarioModificacion != null ? usuarioModificacion : 1);
    }
}

