package com.almacen.warnes.modulo_analitica.controller;

import com.almacen.warnes.modulo_analitica.dto.GenerarReportePeriodoDTO;
import com.almacen.warnes.modulo_analitica.dto.ReporteDetalleDTO;
import com.almacen.warnes.modulo_analitica.dto.ReporteListadoDTO;
import com.almacen.warnes.modulo_analitica.service.ReporteService;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Analítica - Reportes y Libro Mayor", description = "Endpoints para consulta histórica y detalle auditado de reportes de arqueo")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(summary = "Listar reportes de arqueo (Libro Mayor)", description = "Consulta fn_reportes_listar con filtros por sucursal, período, rango de fechas y cajero.")
    @GetMapping
    public ResponseEntity<?> listarReportes(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la sucursal") @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "Período (Turno, Diario, etc.)") @RequestParam(required = false) String periodo,
            @Parameter(description = "Fecha desde (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "Usuario cajero") @RequestParam(required = false) Integer usuarioId
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        Usuario authUser = uOpt.orElse(null);

        List<ReporteListadoDTO> lista = reporteService.listarReportes(
                sucursalId,
                periodo,
                fechaDesde,
                fechaHasta,
                usuarioId,
                authUser
        );
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Obtener detalle auditado de un reporte", description = "Consulta fn_reportes_obtener_detalle: cabecera, indicadores de snapshot, partidas de entradas, salidas y movimientos sellados.")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerDetalle(
            @PathVariable("id") Integer reporteId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        Usuario authUser = uOpt.orElse(null);

        Optional<ReporteDetalleDTO> detalleOpt = reporteService.obtenerDetalleReporte(reporteId, authUser);
        if (detalleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reporte no encontrado o sin permisos para consultarlo"));
        }

        return ResponseEntity.ok(detalleOpt.get());
    }

    @Operation(summary = "Generar un reporte financiero consolidado por período", description = "Ejecuta sp_reportes_generar_periodo para consolidar ventas y movimientos en un rango de fechas.")
    @PostMapping("/generar-corte")
    public ResponseEntity<?> generarReportePeriodo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody GenerarReportePeriodoDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            ReporteDetalleDTO reporte = reporteService.generarReportePeriodo(dto, uOpt.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Reporte de período generado con éxito",
                    "reporteId", reporte.getReporteId(),
                    "reporte", reporte
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al generar reporte de período: {}", e.getMessage(), e);
            String msg = e.getMessage() != null && e.getMessage().contains("ERROR:")
                    ? e.getMessage().substring(e.getMessage().indexOf("ERROR:") + 6).split("\n")[0].trim()
                    : "Error al generar el reporte de período";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        }
    }
}
