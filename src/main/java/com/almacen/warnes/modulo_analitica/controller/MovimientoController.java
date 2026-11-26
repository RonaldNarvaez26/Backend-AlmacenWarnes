package com.almacen.warnes.modulo_analitica.controller;

import com.almacen.warnes.modulo_analitica.dto.CrearMovimientoDTO;
import com.almacen.warnes.modulo_analitica.dto.ModificarMovimientoDTO;
import com.almacen.warnes.modulo_analitica.dto.MovimientoListadoDTO;
import com.almacen.warnes.modulo_analitica.service.MovimientoService;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
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

@Tag(name = "Analítica - Movimientos de Caja", description = "Endpoints para registro, consulta, edición y baja lógica de movimientos de entrada y salida")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(summary = "Registrar un movimiento de caja (Entrada o Salida)", description = "Inserta un movimiento y, opcionalmente para Supervisor/Propietario, lo consolida inmediatamente en Libro Mayor.")
    @PostMapping
    public ResponseEntity<?> registrarMovimiento(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CrearMovimientoDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        try {
            Map<String, Object> res = movimientoService.registrarYConsolidarSiAplica(dto, uOpt.get());
            Integer reporteId = (Integer) res.get("reporteId");
            String msg = reporteId != null
                    ? "Movimiento registrado y consolidado en Libro Mayor (R-" + reporteId + ")"
                    : "Movimiento registrado con éxito";
            res.put("mensaje", msg);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validación fallida en movimiento: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al registrar movimiento: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", extraerMensajeLimpio(e)));
        }
    }

    @Operation(summary = "Consolidar movimientos pendientes en Libro Mayor", description = "Permite a Supervisor o Propietario consolidar movimientos administrativos o de turnos cerrados sin arqueo, generando un reporte en Libro Mayor.")
    @PostMapping("/consolidar")
    public ResponseEntity<?> consolidarMovimientos(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ConsolidarMovimientosRequest request
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        try {
            List<Integer> reportesIds = movimientoService.consolidarMovimientos(request.getMovimientoIds(), uOpt.get());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Movimientos consolidados en Libro Mayor correctamente",
                    "reporteIds", reportesIds
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al consolidar movimientos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", extraerMensajeLimpio(e)));
        }
    }

    @Operation(summary = "Listar movimientos de caja con filtros", description = "Consulta movimientos mediante fn_movimientos_listar aplicando restricciones de sucursal según el rol.")
    @GetMapping
    public ResponseEntity<?> listarMovimientos(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la sucursal") @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "ID del reporte (arqueo)") @RequestParam(required = false) Integer reporteId,
            @Parameter(description = "Solo movimientos pendientes") @RequestParam(required = false) Boolean soloPendientes,
            @Parameter(description = "Tipo de movimiento (1=Entrada, 2=Salida)") @RequestParam(required = false) Integer tipoMovimiento,
            @Parameter(description = "Usuario creador") @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Fecha desde (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "Filtro de estado: PENDIENTES, CONSOLIDADOS o TODOS") @RequestParam(required = false) String estado
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        Usuario authUser = uOpt.orElse(null);

        List<MovimientoListadoDTO> lista = movimientoService.listarMovimientos(
                sucursalId,
                reporteId,
                soloPendientes,
                tipoMovimiento,
                usuarioId,
                fechaDesde,
                fechaHasta,
                estado,
                authUser
        );
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Modificar un movimiento pendiente", description = "Actualiza un movimiento invocando sp_movimientos_modificar. Falla con 409 si el movimiento ya fue sellado en un arqueo.")
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarMovimiento(
            @PathVariable("id") Integer movimientoId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ModificarMovimientoDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        try {
            movimientoService.modificarMovimiento(movimientoId, dto, uOpt.get());
            return ResponseEntity.ok(Map.of("mensaje", "Movimiento modificado correctamente"));
        } catch (Exception e) {
            String msg = extraerMensajeLimpio(e);
            if (msg.contains("sellado") || msg.contains("no existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    @Operation(summary = "Eliminar lógicamente un movimiento pendiente", description = "Marca marca_baja = true invocando sp_movimientos_eliminar. Falla con 409 si el movimiento ya está sellado en un arqueo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMovimiento(
            @PathVariable("id") Integer movimientoId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        try {
            movimientoService.eliminarMovimiento(movimientoId, uOpt.get());
            return ResponseEntity.ok(Map.of("mensaje", "Movimiento eliminado exitosamente"));
        } catch (Exception e) {
            String msg = extraerMensajeLimpio(e);
            if (msg.contains("sellado") || msg.contains("arqueo cerrado") || msg.contains("no existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    private String extraerMensajeLimpio(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("ERROR:")) {
            int start = msg.indexOf("ERROR:") + 6;
            int end = msg.indexOf("\n", start);
            if (end == -1) end = msg.length();
            return msg.substring(start, end).trim();
        }
        return msg != null ? msg : "Error al procesar el movimiento.";
    }

    @Data
    public static class ConsolidarMovimientosRequest {
        @NotEmpty(message = "Debe enviar al menos un ID de movimiento")
        private List<Integer> movimientoIds;
    }
}
