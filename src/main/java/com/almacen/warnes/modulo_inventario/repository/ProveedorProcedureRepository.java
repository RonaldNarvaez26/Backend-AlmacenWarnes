package com.almacen.warnes.modulo_inventario.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;

@Repository
@RequiredArgsConstructor
public class ProveedorProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Ejecuta el procedimiento almacenado spProveedorActualizar
     */
    public void ejecutarActualizarProveedor(
            Integer proveedorId,
            String nombreEmpresa,
            String nombreContacto,
            String numeroContacto,
            String anotaciones,
            Integer usuarioModificacion
    ) {
        String sql = "CALL spProveedorActualizar(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            CallableStatement cs = connection.prepareCall(sql);
            cs.setInt(1, proveedorId);
            cs.setString(2, nombreEmpresa != null ? nombreEmpresa.trim() : "");
            cs.setString(3, nombreContacto != null ? nombreContacto.trim() : "");
            cs.setString(4, numeroContacto != null ? numeroContacto.trim() : "");
            cs.setString(5, anotaciones != null ? anotaciones.trim() : "Sin anotaciones contractuales");
            if (usuarioModificacion != null) {
                cs.setInt(6, usuarioModificacion);
            } else {
                cs.setInt(6, 1);
            }
            return cs;
        });
    }

    /**
     * Ejecuta el procedimiento almacenado spProveedorEliminar (baja logica)
     */
    public void ejecutarEliminarProveedor(Integer proveedorId, Integer usuarioModificacion) {
        String sql = "CALL spProveedorEliminar(?, ?)";
        jdbcTemplate.update(connection -> {
            CallableStatement cs = connection.prepareCall(sql);
            cs.setInt(1, proveedorId);
            if (usuarioModificacion != null) {
                cs.setInt(2, usuarioModificacion);
            } else {
                cs.setInt(2, 1);
            }
            return cs;
        });
    }
}

