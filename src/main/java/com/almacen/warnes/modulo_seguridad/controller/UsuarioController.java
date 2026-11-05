package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.*;
import com.almacen.warnes.modulo_seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seguridad/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Listar usuarios con buscador y filtros (consume la función PostgreSQL fn_usuarios_listar)
     */
    @GetMapping("/listar-tabla")
    public ResponseEntity<?> listarConFiltros(
            @RequestParam(required = false, defaultValue = "false") Boolean mostrarBajas,
            @RequestParam(required = false) Integer rolId,
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(required = false) String textoBusqueda
    ) {
        log.info("Listando usuarios con filtros -> mostrarBajas: {}, rolId: {}, sucursalId: {}, textoBusqueda: '{}'", 
                mostrarBajas, rolId, sucursalId, textoBusqueda != null ? textoBusqueda : "");
        try {
            List<UsuarioListadoDTO> resultado = usuarioService.listarConFiltros(mostrarBajas, rolId, sucursalId, textoBusqueda);
            log.info("Se encontraron {} usuarios con los filtros aplicados", resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al listar usuarios con filtros en PostgreSQL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar usuarios: " + e.getMessage()));
        }
    }

    /**
     * Listar usuarios activos simples
     */
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        log.info("Consultando lista general de usuarios activos");
        List<UsuarioDTO> activos = usuarioService.listarUsuariosActivos();
        log.info("Total usuarios activos encontrados: {}", activos.size());
        return ResponseEntity.ok(activos);
    }

    /**
     * Obtener usuario por ID (solo activos con marcaBaja = false)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        log.info("Buscando usuario activo con ID: {}", id);
        UsuarioDTO dto = usuarioService.obtenerPorId(id);
        if (dto == null) {
            log.warn("Usuario con ID: {} no encontrado o se encuentra dado de baja (marca_baja = true)", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", HttpStatus.NOT_FOUND.value(),
                    "error", "Not Found",
                    "mensaje", "Usuario no encontrado o se encuentra inactivo (ID: " + id + ")"
            ));
        }
        log.info("Usuario encontrado: ID {}, usuario: '{}', rolId: {}", dto.getUsuarioId(), dto.getUsuario(), dto.getRolId());
        return ResponseEntity.ok(dto);
    }

    /**
     * Crear nuevo usuario (llama al procedure sp_usuarios_insertar cifrando con BCrypt)
     */
    @PostMapping
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody UsuarioRegistroDTO dto) {
        log.info("Solicitud para registrar nuevo usuario: '{}', rolId: {}, sucursalId: {}, usuarioCreacion: {}", 
                dto.getUsuario(), dto.getRolId(), dto.getSucursalId(), dto.getUsuarioCreacion());
        try {
            Integer nuevoId = usuarioService.registrarUsuario(dto);
            log.info("Usuario '{}' registrado exitosamente con ID asignado: {}", dto.getUsuario(), nuevoId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "Usuario registrado exitosamente");
            resp.put("usuarioId", nuevoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al registrar usuario '{}': {}", dto.getUsuario(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Excepción al registrar usuario '{}': {}", dto.getUsuario(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar usuario: " + e.getMessage()));
        }
    }

    /**
     * Modificar usuario existente (llama al procedure sp_usuarios_modificar)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioModificarDTO dto
    ) {
        log.info("Solicitud para modificar usuario ID: {}, por usuarioModificador: {}", id, dto.getUsuarioModificacion());
        try {
            usuarioService.modificarUsuario(id, dto);
            log.info("Usuario ID: {} modificado exitosamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario modificado exitosamente"));
        } catch (Exception e) {
            log.error("Error al modificar usuario ID: {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al modificar usuario: " + e.getMessage()));
        }
    }

    /**
     * Eliminar lógicamente a un usuario (llama al procedure sp_usuarios_eliminar)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @PathVariable Integer id,
            @RequestParam Integer usuarioModificacion
    ) {
        log.info("Solicitud para dar de baja lógica al usuario ID: {} por usuarioModificacion: {}", id, usuarioModificacion);
        try {
            usuarioService.eliminarUsuario(id, usuarioModificacion);
            log.info("Usuario ID: {} dado de baja lógica exitosamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario dado de baja exitosamente"));
        } catch (Exception e) {
            log.error("Error al dar de baja al usuario ID: {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al dar de baja el usuario: " + e.getMessage()));
        }
    }
}
