package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.CambiarContrasenaDTO;
import com.almacen.warnes.modulo_seguridad.dto.LoginRequestDTO;
import com.almacen.warnes.modulo_seguridad.dto.LoginResponseDTO;
import com.almacen.warnes.modulo_seguridad.dto.RecuperarContrasenaDTO;
import com.almacen.warnes.modulo_seguridad.dto.SolicitarRecuperacionDTO;
import com.almacen.warnes.modulo_seguridad.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Seguridad - Autenticación", description = "Endpoints de login con JWT y BCrypt, solicitud y reseteo de contraseñas y cambio voluntario de credenciales")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/seguridad/auth")
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;

    @Operation(
            summary = "Iniciar sesión (Login)",
            description = "Valida el nombre de usuario y contraseña en PostgreSQL comparando el hash BCrypt, verifica que la cuenta se encuentre activa (estado = 1) y genera un token JWT firmado junto a los datos del rol y sucursal asignada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta de usuario bloqueada"),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado durante la autenticación")
    })
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

    @Operation(
            summary = "Solicitar recuperación de contraseña (Empleado/Cajero)",
            description = "Invoca el procedimiento almacenado `CALL sp_usuarios_solicitar_recuperacion(usuario)`. Cambia el estado del usuario a 2 ('Solicita Recuperación') para que el propietario visualice la alerta en su panel de control."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud de recuperación registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "El usuario indicado no existe o se encuentra inactivo"),
            @ApiResponse(responseCode = "500", description = "Error interno al procesar la solicitud")
    })
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

    @Operation(
            summary = "Restablecer contraseña de usuario (Propietario)",
            description = "Permite a un usuario con rol de Propietario asignar una nueva contraseña a un empleado. La contraseña se hashea con BCrypt y el estado de la cuenta se restaura a 1 ('Activo')."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Usuario no encontrado o datos de solicitud inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno al restablecer la contraseña")
    })
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

    @Operation(
            summary = "Cambiar contraseña voluntariamente (Usuario en sesión)",
            description = "Permite al usuario logueado cambiar su contraseña verificando previamente que la contraseña actual ingresada coincida con el hash almacenado en PostgreSQL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada erróneos o usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "La contraseña actual no coincide"),
            @ApiResponse(responseCode = "500", description = "Error interno al actualizar la contraseña")
    })
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
