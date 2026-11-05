package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.CambiarContrasenaDTO;
import com.almacen.warnes.modulo_seguridad.dto.LoginRequestDTO;
import com.almacen.warnes.modulo_seguridad.dto.LoginResponseDTO;
import com.almacen.warnes.modulo_seguridad.dto.RecuperarContrasenaDTO;
import com.almacen.warnes.modulo_seguridad.dto.SolicitarRecuperacionDTO;
import com.almacen.warnes.modulo_seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seguridad/auth")
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;

    /**
     * Endpoint de autenticación (Login)
     * Valida el hash en PostgreSQL con BCrypt y genera el JWT
     * Retorna el token y el usuario_id para consultas posteriores
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto) {
        log.info("Intento de inicio de sesión para el usuario: '{}'", dto.getUsuario());
        try {
            LoginResponseDTO response = usuarioService.login(dto);
            log.info("Inicio de sesión exitoso -> usuario: '{}', ID: {}, sucursalId: {}, rolId: {}, estado: {}", 
                    response.getUsuario(), response.getUsuarioId(), response.getSucursalId(), response.getRolId(), response.getEstado());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Fallo de autenticación para usuario '{}': {}", dto.getUsuario(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error no controlado durante el inicio de sesión para '{}': {}", dto.getUsuario(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error durante el inicio de sesión: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para que un empleado/usuario solicite recuperación de contraseña
     * Invoca sp_usuarios_solicitar_recuperacion y cambia su estado a 2 ('Solicita Recuperación')
     */
    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO dto) {
        log.info("Usuario '{}' ha solicitado recuperación de contraseña", dto.getUsuario());
        try {
            usuarioService.solicitarRecuperacion(dto.getUsuario());
            log.info("Solicitud registrada correctamente para '{}', estado cambiado a 2 (Solicita Recuperación)", dto.getUsuario());
            return ResponseEntity.ok(Map.of("mensaje", "Solicitud de recuperación enviada. El propietario revisará su solicitud."));
        } catch (IllegalArgumentException e) {
            log.warn("Error en solicitud de recuperación para '{}': {}", dto.getUsuario(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al procesar solicitud de recuperación para '{}': {}", dto.getUsuario(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para recuperar/restablecer contraseña por nombre de usuario (ejecutado por Propietario)
     */
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<?> recuperarContrasena(@Valid @RequestBody RecuperarContrasenaDTO dto) {
        log.info("Propietario/Admin (ID: {}) restableciendo contraseña para usuario: '{}'", 
                dto.getUsuarioModificacion(), dto.getUsuario());
        try {
            usuarioService.recuperarContrasena(dto);
            log.info("Contraseña restablecida con éxito para usuario: '{}', estado restaurado a 1 (Activo)", dto.getUsuario());
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
        } catch (IllegalArgumentException e) {
            log.warn("Fallo al restablecer contraseña para '{}': {}", dto.getUsuario(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al restablecer contraseña para '{}': {}", dto.getUsuario(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al recuperar la contraseña: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para que un usuario cambie su propia contraseña
     */
    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@Valid @RequestBody CambiarContrasenaDTO dto) {
        log.info("Usuario ID: {} solicitando cambio voluntario de contraseña", dto.getUsuarioId());
        try {
            usuarioService.cambiarContrasena(dto);
            log.info("Contraseña cambiada exitosamente para usuario ID: {}", dto.getUsuarioId());
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña cambiada exitosamente"));
        } catch (BadCredentialsException e) {
            log.warn("Contraseña actual incorrecta para usuario ID: {}", dto.getUsuarioId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Usuario no encontrado al intentar cambiar contraseña (ID: {})", dto.getUsuarioId());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al cambiar contraseña para usuario ID {}: {}", dto.getUsuarioId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar la contraseña: " + e.getMessage()));
        }
    }
}
