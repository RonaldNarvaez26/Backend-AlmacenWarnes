package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.SesionDTO;
import com.almacen.warnes.modulo_seguridad.dto.SesionListadoDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.service.SesionService;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Seguridad - Sesiones y Turnos", description = "Endpoints para consulta de turno activo, cierre directo de sesión y auditoría histórica con alertas")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private final SesionService sesionService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(summary = "Obtener sesión/turno activo del usuario", description = "Devuelve la sesión activa no cerrada del usuario autenticado vía fn_sesiones_obtener_activa.")
    @GetMapping("/activa")
    public ResponseEntity<?> obtenerSesionActiva(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "usuarioId", required = false) Integer usuarioIdParam
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o no autenticada"));
        }

        Integer usuarioId = uOpt.get().getUsuarioId();
        Optional<SesionDTO> sesionOpt = sesionService.obtenerSesionActiva(usuarioId);
        if (sesionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No hay turno activo para el usuario"));
        }

        return ResponseEntity.ok(sesionOpt.get());
    }

    @Operation(summary = "Cerrar sesión directa", description = "Cierra la sesión activa del usuario invocando sp_sesiones_cerrar. Utilizado por Propietarios/Supervisores, o por Cajeros sin actividad o que confirman omitir arqueo.")
    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarSesionDirecta(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Integer usuarioIdParam = null;
        Integer sesionIdParam = null;
        if (body != null) {
            if (body.get("usuarioId") != null) usuarioIdParam = ((Number) body.get("usuarioId")).intValue();
            if (body.get("sesionId") != null) sesionIdParam = ((Number) body.get("sesionId")).intValue();
        }

        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        Integer usuarioId = uOpt.get().getUsuarioId();

        // Si no se proporcionó sesionId, buscar la activa
        if (sesionIdParam == null) {
            Optional<SesionDTO> activa = sesionService.obtenerSesionActiva(usuarioId);
            if (activa.isPresent()) {
                sesionIdParam = activa.get().getSesionId();
            } else {
                return ResponseEntity.ok(Map.of("mensaje", "No había ninguna sesión activa para cerrar."));
            }
        }

        try {
            Integer duracion = sesionService.cerrarSesionDirecta(sesionIdParam, usuarioId);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Sesión cerrada correctamente",
                    "sesionId", sesionIdParam,
                    "duracionMinutos", duracion != null ? duracion : 0
            ));
        } catch (Exception e) {
            log.error("Error al cerrar sesión {}: {}", sesionIdParam, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Auditoría de sesiones históricas", description = "Lista las sesiones registradas con información de duración, estado de arqueo y alerta_sin_arqueo.")
    @GetMapping
    public ResponseEntity<?> listarSesiones(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Filtrar por usuario") @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Filtrar por sucursal") @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "Solo sesiones activas") @RequestParam(required = false) Boolean soloActivas,
            @Parameter(description = "Fecha desde (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "Búsqueda por texto en usuario o nombres") @RequestParam(required = false) String textoBusqueda
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        Usuario usuarioAuth = uOpt.orElse(null);

        // Si es cajero (rol 1), solo puede consultar sus propias sesiones
        if (usuarioAuth != null && usuarioAuth.getRolId() == 1) {
            usuarioId = usuarioAuth.getUsuarioId();
        }

        List<SesionListadoDTO> lista = sesionService.listarSesionesAuditoria(
                usuarioId,
                sucursalId,
                soloActivas,
                fechaDesde,
                fechaHasta,
                textoBusqueda,
                usuarioAuth
        );
        return ResponseEntity.ok(lista);
    }
}
