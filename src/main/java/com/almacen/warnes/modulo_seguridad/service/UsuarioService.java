package com.almacen.warnes.modulo_seguridad.service;

import com.almacen.warnes.modulo_seguridad.dto.UsuarioDTO;
import com.almacen.warnes.modulo_seguridad.mapper.UsuarioMapper;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
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
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toDTO)
                .orElse(null);
    }
}
