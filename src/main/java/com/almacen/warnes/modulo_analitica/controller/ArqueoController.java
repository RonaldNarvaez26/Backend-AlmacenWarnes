package com.almacen.warnes.modulo_analitica.controller;

import com.almacen.warnes.modulo_analitica.dto.ArqueoResumenTurnoDTO;
import com.almacen.warnes.modulo_analitica.dto.RegistrarArqueoDTO;
import com.almacen.warnes.modulo_analitica.service.ReporteService;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Tag(name = "Analítica - Arqueo de Caja del Turno", description = "Endpoints para consulta de resumen de turno del cajero y registro de arqueo con cierre atómico de sesión")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/arqueo")
@RequiredArgsConstructor
public class ArqueoController {

    private final ReporteService reporteService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(
            summary = "Obtener resumen del turno activo (Cajero)",
            description = "Consulta fn_arqueo_resumen_turno para verificar si el cajero requiere arqueo (ventas o movimientos en el turno) y obtener los totales desagregados."
    )
    @GetMapping("/resumen-turno")
    public ResponseEntity<?> obtenerResumenTurno(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        Integer usuarioId = uOpt.get().getUsuarioId();
        Optional<ArqueoResumenTurnoDTO> resumenOpt = reporteService.obtenerResumenTurno(usuarioId);
        if (resumenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No hay turno activo para el usuario"));
        }

        return ResponseEntity.ok(resumenOpt.get());
    }

    @Operation(
            summary = "Registrar arqueo de caja y cerrar sesión",
            description = "Ejecuta sp_reportes_registrar_arqueo. Crea el reporte, inserta entradas y salidas, sella movimientos del turno, consolida en Opción B y cierra la sesión atómicamente."
    )
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarArqueo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody RegistrarArqueoDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida"));
        }

        try {
            Integer reporteId = reporteService.registrarArqueo(dto, uOpt.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Arqueo de caja registrado y sesión cerrada con éxito",
                    "reporteId", reporteId
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de validación al registrar arqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al registrar arqueo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", extraerMensajeLimpio(e)));
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
        return msg != null ? msg : "Error al procesar el arqueo de caja.";
    }
}
