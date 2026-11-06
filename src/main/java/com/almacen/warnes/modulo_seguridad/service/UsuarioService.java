package com.almacen.warnes.modulo_seguridad.service;

import com.almacen.warnes.modulo_seguridad.dto.UsuarioConRolProjection;
import com.almacen.warnes.modulo_seguridad.dto.UsuarioDTO;
import com.almacen.warnes.modulo_seguridad.mapper.UsuarioMapper;
import com.almacen.warnes.modulo_seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public List<UsuarioDTO> listarUsuariosActivos() {
        List<UsuarioConRolProjection> projections = usuarioRepository.listarUsuariosActivosConRol();
        if (projections != null && !projections.isEmpty()) {
            return projections.stream()
                    .map(this::toDTOFromProjection)
                    .collect(Collectors.toList());
        }
        return usuarioRepository.findByMarcaBajaFalse().stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO obtenerPorId(Integer id) {
        return usuarioRepository.obtenerUsuarioConRolPorId(id)
                .map(this::toDTOFromProjection)
                .orElseGet(() -> usuarioRepository.findById(id).map(usuarioMapper::toDTO).orElse(null));
    }

    private UsuarioDTO toDTOFromProjection(UsuarioConRolProjection p) {
        return UsuarioDTO.builder()
                .usuarioId(p.getUsuarioId())
                .usuario(p.getUsuario())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .rolId(p.getRolId())
                .rolNombre(p.getRolNombre())
                .nombreCompleto(p.getNombreCompleto())
                .marcaBaja(p.getMarcaBaja())
                .fechaCreacion(p.getFechaCreacion())
                .usuarioCreacion(p.getUsuarioCreacion())
                .fechaModificacion(p.getFechaModificacion())
                .usuarioModificacion(p.getUsuarioModificacion())
                .build();
    }
}
