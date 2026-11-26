package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.*;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.service.UsuarioService;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Seguridad - Usuarios", description = "Gestión de cuentas de usuarios, asignación de roles, control de sucursales y auditoría")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping({ "/api/seguridad/usuarios", "/api/usuarios" })
@RequiredArgsConstructor
public class UsuarioController {

        private final UsuarioService usuarioService;
        private final SecurityContextHelper securityContextHelper;

        @Operation(summary = "Listar usuarios con filtros dinámicos y buscador", description = "Ejecuta la función almacenada `fn_usuarios_listar(p_mostrar_bajas, p_rol_id, p_sucursal_id, p_texto_busqueda)` en PostgreSQL. Retorna los usuarios con su rol paramétrico, sucursal, estado y fecha de creación.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de usuarios obtenido exitosamente"),
                        @ApiResponse(responseCode = "500", description = "Error interno al consultar la función en PostgreSQL")
        })
        @GetMapping("/listar-tabla")
        public ResponseEntity<?> listarConFiltros(
                        @Parameter(description = "Indica si se deben incluir usuarios dados de baja lógica (marca_baja = true)", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean mostrarBajas,
                        @Parameter(description = "Filtrar por rol: 1=Cajero, 2=Supervisor, 3=Propietario", example = "1") @RequestParam(required = false) Integer rolId,
                        @Parameter(description = "Filtrar por sucursal específica", example = "1") @RequestParam(required = false) Integer sucursalId,
                        @Parameter(description = "Texto para buscar coincidencias en username, nombres o apellidos", example = "carlos") @RequestParam(required = false) String textoBusqueda) {
                log.info("Listando usuarios con filtros -> mostrarBajas: {}, rolId: {}, sucursalId: {}, textoBusqueda: '{}'",
                                mostrarBajas, rolId, sucursalId, textoBusqueda != null ? textoBusqueda : "");
                try {
                        List<UsuarioListadoDTO> resultado = usuarioService.listarConFiltros(mostrarBajas, rolId,
                                        sucursalId, textoBusqueda);
                        log.info("Se encontraron {} usuarios con los filtros aplicados", resultado.size());
                        return ResponseEntity.ok(resultado);
                } catch (Exception e) {
                        log.error("Error al listar usuarios con filtros en PostgreSQL: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Error al consultar usuarios: " + e.getMessage()));
                }
        }

        @Operation(summary = "Listar usuarios activos simples", description = "Consulta JPA directa a la tabla usuarios filtrando por `marca_baja = false`.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado general de usuarios activos")
        })
        @CrossOrigin(origins = "*")
        @GetMapping
        public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
                log.info("Consultando lista general de usuarios activos");
                List<UsuarioDTO> activos = usuarioService.listarUsuariosActivos();
                log.info("Total usuarios activos encontrados: {}", activos.size());
                return ResponseEntity.ok(activos);
        }

        @Operation(summary = "Obtener usuario activo por ID", description = "Recupera los datos del usuario activo especificado por su identificador único.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario encontrado", content = @Content(schema = @Schema(implementation = UsuarioDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado o se encuentra dado de baja")
        })
        @GetMapping("/{id}")
        public ResponseEntity<?> obtenerPorId(
                        @Parameter(description = "Identificador único del usuario", example = "1", required = true) @PathVariable Integer id) {
                log.info("Buscando usuario activo con ID: {}", id);
                UsuarioDTO dto = usuarioService.obtenerPorId(id);
                if (dto == null) {
                        log.warn("Usuario con ID: {} no encontrado o se encuentra dado de baja (marca_baja = true)",
                                        id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                        "timestamp", LocalDateTime.now().toString(),
                                        "status", HttpStatus.NOT_FOUND.value(),
                                        "error", "Not Found",
                                        "mensaje", "Usuario no encontrado o se encuentra inactivo (ID: " + id + ")"));
                }
                log.info("Usuario encontrado: ID {}, usuario: '{}', rolId: {}", dto.getUsuarioId(), dto.getUsuario(),
                                dto.getRolId());
                return ResponseEntity.ok(dto);
        }

        @Operation(summary = "Registrar nuevo usuario", description = "Invoca el procedimiento almacenado transaccional `CALL sp_usuarios_insertar(...)`. Hashea la contraseña con BCrypt, asigna el rol y la sucursal (opcional para propietarios).")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Validación fallida o nombre de usuario ya existente en el sistema"),
                        @ApiResponse(responseCode = "500", description = "Error interno al ejecutar el procedimiento en PostgreSQL")
        })
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
                                        "error", e.getMessage()));
                } catch (Exception e) {
                        log.error("Excepción al registrar usuario '{}': {}", dto.getUsuario(), e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Error al registrar usuario: " + e.getMessage()));
                }
        }

        @Operation(summary = "Modificar usuario existente", description = "Invoca el procedimiento almacenado `CALL sp_usuarios_modificar(...)`. Actualiza nombres, apellidos, rol y sucursal sin modificar la contraseña.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario modificado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado o dado de baja"),
                        @ApiResponse(responseCode = "500", description = "Error interno al modificar usuario")
        })
        @PutMapping("/{id}")
        public ResponseEntity<?> modificarUsuario(
                        @Parameter(description = "ID del usuario a modificar", example = "1", required = true) @PathVariable Integer id,
                        @Valid @RequestBody UsuarioModificarDTO dto) {
                log.info("Solicitud para modificar usuario ID: {}, por usuarioModificador: {}", id,
                                dto.getUsuarioModificacion());
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

        @Operation(summary = "Bloquear o desbloquear una cuenta de usuario", description = "Cambia el estado de cuenta del usuario entre Activo (1) y Bloqueado (3) invocando sp_usuarios_cambiar_estado. Impide auto-bloqueo y el bloqueo del último propietario activo.")
        @PatchMapping("/{id}/estado")
        public ResponseEntity<?> cambiarEstadoUsuario(
                        @Parameter(description = "ID del usuario a actualizar", example = "5", required = true) @PathVariable Integer id,
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @Valid @RequestBody CambiarEstadoUsuarioDTO dto) {
                Optional<Usuario> authOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
                if (authOpt.isPresent() && (authOpt.get().getRolId() == null || authOpt.get().getRolId() != 3)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error",
                                                        "Solo el Propietario puede bloquear o desbloquear cuentas."));
                }

                Integer operadorId = authOpt.map(Usuario::getUsuarioId).orElse(dto.getUsuarioModificacion());
                log.info("Solicitud para cambiar estado del usuario ID: {} a estado {} por operador ID: {}",
                                id, dto.getEstado(), operadorId);
                try {
                        usuarioService.cambiarEstadoUsuario(id, dto.getEstado(), operadorId);
                        String mensaje = dto.getEstado() == 3
                                        ? "Cuenta de usuario bloqueada exitosamente"
                                        : "Cuenta de usuario desbloqueada y activada exitosamente";
                        return ResponseEntity.ok(Map.of("mensaje", mensaje, "estado", dto.getEstado()));
                } catch (IllegalArgumentException e) {
                        log.warn("Validación rechazada al cambiar estado de usuario ID {}: {}", id, e.getMessage());
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                        String msg = extraerMensajeLimpio(e);
                        log.error("Error al cambiar estado de usuario ID {}: {}", id, msg);
                        return ResponseEntity.badRequest().body(Map.of("error", msg));
                }
        }

        @Operation(summary = "Dar de baja lógica a un usuario", description = "Invoca el procedimiento almacenado `CALL sp_usuarios_eliminar(...)`. Establece `marca_baja = true` y registra la autoría de modificación sin eliminar el registro físicamente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Usuario dado de baja exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Operación no permitida (auto-baja o último propietario)"),
                        @ApiResponse(responseCode = "500", description = "Error interno al dar de baja el usuario")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<?> eliminarUsuario(
                        @Parameter(description = "ID del usuario a eliminar lógicamente", example = "1", required = true) @PathVariable Integer id,
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @Parameter(description = "ID del usuario que realiza la operación", example = "3") @RequestParam(required = false) Integer usuarioModificacion) {
                Optional<Usuario> authOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
                if (authOpt.isPresent() && (authOpt.get().getRolId() == null || authOpt.get().getRolId() != 3)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error", "Solo el Propietario puede dar de baja cuentas."));
                }
                Integer operadorId = authOpt.map(Usuario::getUsuarioId).orElse(usuarioModificacion);
                log.info("Solicitud para dar de baja lógica al usuario ID: {} por operadorId: {}", id, operadorId);
                try {
                        usuarioService.eliminarUsuario(id, operadorId);
                        log.info("Usuario ID: {} dado de baja lógica exitosamente", id);
                        return ResponseEntity.ok(Map.of("mensaje", "Usuario dado de baja exitosamente"));
                } catch (IllegalArgumentException e) {
                        log.warn("Validación rechazada al dar de baja usuario ID {}: {}", id, e.getMessage());
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                        String msg = extraerMensajeLimpio(e);
                        log.error("Error al dar de baja al usuario ID: {}: {}", id, msg);
                        return ResponseEntity.badRequest().body(Map.of("error", msg));
                }
        }

        private String extraerMensajeLimpio(Exception e) {
                String msg = e.getMessage();
                if (msg != null && msg.contains("ERROR:")) {
                        int start = msg.indexOf("ERROR:") + 6;
                        int end = msg.indexOf("\n", start);
                        if (end == -1)
                                end = msg.length();
                        return msg.substring(start, end).trim();
                }
                return msg != null ? msg : "Error al procesar la operación de usuario.";
        }
}