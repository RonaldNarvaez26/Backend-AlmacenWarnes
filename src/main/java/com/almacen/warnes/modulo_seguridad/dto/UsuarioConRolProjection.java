package com.almacen.warnes.modulo_seguridad.dto;

import java.time.LocalDateTime;

public interface UsuarioConRolProjection {
    Integer getUsuarioId();
    String getUsuario();
    String getNombres();
    String getApellidos();
    Integer getRolId();
    String getRolNombre();
    String getNombreCompleto();
    Boolean getMarcaBaja();
    LocalDateTime getFechaCreacion();
    Integer getUsuarioCreacion();
    LocalDateTime getFechaModificacion();
    Integer getUsuarioModificacion();
}

