package com.almacen.warnes.modulo_seguridad.mapper;

import com.almacen.warnes.modulo_seguridad.dto.UsuarioDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioDTO toDTO(Usuario entity) {
        if (entity == null) return null;
        return UsuarioDTO.builder()
                .usuarioId(entity.getUsuarioId())
                .usuario(entity.getUsuario())
                .nombres(entity.getNombres())
                .apellidos(entity.getApellidos())
                .rolId(entity.getRolId())
                .marcaBaja(entity.getMarcaBaja())
                .fechaCreacion(entity.getFechaCreacion())
                .usuarioCreacion(entity.getUsuarioCreacion())
                .fechaModificacion(entity.getFechaModificacion())
                .usuarioModificacion(entity.getUsuarioModificacion())
                .build();
    }

    public Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;
        return Usuario.builder()
                .usuarioId(dto.getUsuarioId())
                .usuario(dto.getUsuario())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .rolId(dto.getRolId())
                .marcaBaja(dto.getMarcaBaja())
                .fechaCreacion(dto.getFechaCreacion())
                .usuarioCreacion(dto.getUsuarioCreacion())
                .fechaModificacion(dto.getFechaModificacion())
                .usuarioModificacion(dto.getUsuarioModificacion())
                .build();
    }
}
