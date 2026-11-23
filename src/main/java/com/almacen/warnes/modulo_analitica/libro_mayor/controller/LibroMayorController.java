package com.almacen.warnes.modulo_analitica.libro_mayor.controller;

import com.almacen.warnes.modulo_analitica.libro_mayor.dto.AnularMovimientoRequestDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.FiltrosOpcionesDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorItemDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorResumenDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.MovimientoExtraVentaRequestDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.service.LibroMayorService;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Analítica - Libro Mayor", description = "Endpoints para consulta de movimientos, resumen consolidado, recibos de venta, opciones de filtros y bajas")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/libro-mayor")
@RequiredArgsConstructor
public class LibroMayorController {

    private final LibroMayorService libroMayorService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(summary = "Consultar movimientos y bajas del Libro Mayor", description = "Retorna el listado cronológico con filtros temporales (fecha/hora) y por entidad (cajero/producto).")
    @GetMapping
    public ResponseEntity<?> consultarLibroMayor(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la sucursal (opcional para Propietario)")
            @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "Fecha inicial del rango (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha final del rango (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Hora inicial del rango (HH:mm o HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @Parameter(description = "Hora final del rango (HH:mm o HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @Parameter(description = "ID del cajero/usuario para filtrar sus movimientos")
            @RequestParam(required = false) Integer cajeroId,
            @Parameter(description = "ID del producto para filtrar ventas que lo contengan")
            @RequestParam(required = false) Integer productoId,
            @Parameter(description = "Indica si se deben incluir bajas y ventas anuladas")
            @RequestParam(required = false, defaultValue = "true") Boolean incluirBajas,
            @Parameter(description = "Filtrar por tipo (1 = Entrada, 2 = Salida)")
            @RequestParam(required = false) Integer tipoMovimiento,
            @Parameter(description = "Filtrar por método de pago (1 = Efectivo, 2 = QR, 3 = Tarjeta, etc.)")
            @RequestParam(required = false) Integer metodoMovimiento
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            List<LibroMayorItemDTO> items = libroMayorService.consultarLibroMayor(
                    sucursalId,
                    fechaInicio,
                    fechaFin,
                    horaInicio,
                    horaFin,
                    cajeroId,
                    productoId,
                    tipoMovimiento,
                    metodoMovimiento,
                    incluirBajas,
                    uOpt.get()
            );
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException e) {
            log.warn("Parámetros inválidos en consulta de Libro Mayor: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al consultar Libro Mayor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al consultar Libro Mayor."));
        }
    }

    @Operation(summary = "Consultar resumen y balance del Libro Mayor", description = "Calcula totales de ventas en QR, Efectivo, Tarjeta, movimientos extra-venta, bajas y saldo neto.")
    @GetMapping("/resumen")
    public ResponseEntity<?> consultarResumen(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID de la sucursal (opcional para Propietario)")
            @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "Fecha inicial del rango (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha final del rango (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Hora inicial del rango (HH:mm)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @Parameter(description = "Hora final del rango (HH:mm)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @Parameter(description = "ID del cajero/usuario")
            @RequestParam(required = false) Integer cajeroId,
            @Parameter(description = "ID del producto")
            @RequestParam(required = false) Integer productoId
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            LibroMayorResumenDTO resumen = libroMayorService.consultarResumen(
                    sucursalId,
                    fechaInicio,
                    fechaFin,
                    horaInicio,
                    horaFin,
                    cajeroId,
                    productoId,
                    uOpt.get()
            );
            return ResponseEntity.ok(resumen);
        } catch (IllegalArgumentException e) {
            log.warn("Parámetros inválidos en resumen de Libro Mayor: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al obtener resumen de Libro Mayor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al obtener resumen del Libro Mayor."));
        }
    }

    @Operation(summary = "Obtener datos para emisión de Recibo PDF de una venta", description = "Retorna la cabecera completa, datos de cajero y desglose de ítems para comprobante PDF.")
    @GetMapping(value = "/ventas/{ventaId}/recibo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerReciboVenta(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer ventaId
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            String reciboJson = libroMayorService.obtenerReciboVenta(ventaId, uOpt.get());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(reciboJson, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Venta no encontrada para recibo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al obtener recibo de venta #{}: {}", ventaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al generar datos del recibo."));
        }
    }

    @Operation(summary = "Obtener opciones de filtros", description = "Retorna listas de cajeros y productos disponibles para selectores.")
    @GetMapping("/filtros-opciones")
    public ResponseEntity<?> obtenerFiltrosOpciones(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer sucursalId
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            FiltrosOpcionesDTO opciones = libroMayorService.obtenerFiltrosOpciones(sucursalId, uOpt.get());
            return ResponseEntity.ok(opciones);
        } catch (Exception e) {
            log.error("Error al obtener opciones de filtros: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al cargar opciones de filtros."));
        }
    }

    @Operation(summary = "Registrar un movimiento extra-venta", description = "Registra un préstamo, deuda, adelanto a empleado, gasto o ingreso extraordinario.")
    @PostMapping("/movimientos-extra")
    public ResponseEntity<?> registrarMovimientoExtra(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody MovimientoExtraVentaRequestDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        try {
            Map<String, Object> resultado = libroMayorService.registrarMovimientoExtra(dto, uOpt.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Validación fallida en registro extra-venta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al registrar movimiento extra-venta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al registrar el movimiento extra-venta."));
        }
    }

    @Operation(summary = "Anular o dar de baja un movimiento/venta en el Libro Mayor")
    @PutMapping("/movimientos/{id}/anular")
    public ResponseEntity<?> anularMovimiento(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id,
            @RequestBody(required = false) AnularMovimientoRequestDTO dto
    ) {
        Optional<Usuario> uOpt = securityContextHelper.obtenerUsuarioSesion(null, authHeader);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión no válida o expirada"));
        }

        String origen = dto != null && dto.getOrigen() != null ? dto.getOrigen() : "EXTRA_VENTA";
        String motivo = dto != null ? dto.getMotivo() : null;

        try {
            Map<String, Object> resultado = libroMayorService.anularMovimiento(origen, id, motivo, uOpt.get());
            boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
            if (exito) {
                return ResponseEntity.ok(resultado);
            } else {
                return ResponseEntity.badRequest().body(resultado);
            }
        } catch (Exception e) {
            log.error("Error al anular movimiento en Libro Mayor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al anular el registro."));
        }
    }
}
