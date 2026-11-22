package com.almacen.warnes.modulo_notificaciones.controller;

import com.almacen.warnes.modulo_notificaciones.dto.*;
import com.almacen.warnes.modulo_notificaciones.model.Notificacion;
import com.almacen.warnes.modulo_notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "Notificaciones y Alertas Internas", description = "Endpoints para consulta de notificaciones en tiempo real, validación de stock y permisos de dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Listar notificaciones y alertas internas activas",
            description = "Consulta la función PostgreSQL `fn_notificaciones_listar` aplicando RBAC según el rol y sucursal del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado y resumen de alertas obtenido con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno al consultar notificaciones")
    })
    @GetMapping
    public ResponseEntity<?> listarNotificaciones(
            @Parameter(description = "ID de la sucursal a filtrar (solo Propietario; ignorado para Cajero/Supervisor)")
            @RequestParam(required = false) Integer sucursalId,
            @Parameter(description = "ID explícito de usuario si no se utiliza JWT Bearer")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            NotificacionResponseDTO response = notificacionService.obtenerNotificaciones(sucursalId, usuarioId, authHeader);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al obtener notificaciones: " + e.getMessage()));
        }
    }

    @Operation(summary = "Marcar notificación individual como leída",
            description = "Ejecuta el stored procedure `sp_notificaciones_marcar_leido` para marcar una notificación como leída.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
            @ApiResponse(responseCode = "500", description = "Error interno al marcar notificación")
    })
    @RequestMapping(value = {"/{id}/leer", "/{id}/marcar-leido"}, method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> marcarLeido(
            @Parameter(description = "ID de la notificación a marcar como leída", required = true)
            @PathVariable("id") String id,
            @Parameter(description = "ID explícito de usuario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Map<String, Object> resultado = notificacionService.marcarLeido(id, usuarioId, authHeader);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al marcar notificación como leída: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al marcar notificación: " + e.getMessage()));
        }
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas",
            description = "Ejecuta el stored procedure `sp_notificaciones_marcar_todas_leidas` para marcar todas las notificaciones del usuario/rol.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todas las notificaciones fueron marcadas como leídas"),
            @ApiResponse(responseCode = "500", description = "Error interno al marcar notificaciones")
    })
    @RequestMapping(value = {"/marcar-todas-leidas", "/leer-todas"}, method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> marcarTodasLeidas(
            @Parameter(description = "ID explícito de usuario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Map<String, Object> resultado = notificacionService.marcarTodasLeidas(usuarioId, authHeader);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al marcar todas las notificaciones como leídas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al marcar todas las notificaciones: " + e.getMessage()));
        }
    }

    @Operation(summary = "Crear nueva notificación física",
            description = "Registra una notificación formal en la tabla `Notificaciones` clasificada por el Grupo 10 de Conceptos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificación creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de notificación inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno al crear notificación")
    })
    @PostMapping
    public ResponseEntity<?> crearNotificacion(
            @Valid @RequestBody CrearNotificacionRequestDTO request,
            @Parameter(description = "ID explícito de usuario emisor (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Notificacion creada = notificacionService.crearNotificacion(request, usuarioId, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (Exception e) {
            log.error("Error al crear notificación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al crear notificación: " + e.getMessage()));
        }
    }

    @Operation(summary = "Verificar disponibilidad física de stock antes de una venta",
            description = "Consulta la función PostgreSQL `fn_notificaciones_verificar_stock_venta` para verificar disponibilidad y nivel de alerta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación de stock ejecutada con éxito"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno al verificar stock")
    })
    @GetMapping("/verificar-stock")
    public ResponseEntity<?> verificarStockVenta(
            @Parameter(description = "ID de la sucursal donde se pretende realizar la venta", required = true)
            @RequestParam Integer sucursalId,
            @Parameter(description = "ID del producto a vender", required = true)
            @RequestParam Integer productoId,
            @Parameter(description = "Cantidad de unidades que se pretenden vender", required = true)
            @RequestParam BigDecimal cantidad,
            @Parameter(description = "ID explícito de usuario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            VerificacionStockVentaDTO resultado = notificacionService.verificarStockVenta(
                    sucursalId, productoId, cantidad, usuarioId, authHeader);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al verificar stock previo a venta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar stock: " + e.getMessage()));
        }
    }

    @Operation(summary = "Solicitar acceso temporal al Dashboard (Supervisor)",
            description = "Ejecuta el stored procedure `sp_notificaciones_solicitar_dashboard` para registrar la solicitud en Conceptos Grupo 10.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada correctamente"),
            @ApiResponse(responseCode = "400", description = "Petición no válida para el rol"),
            @ApiResponse(responseCode = "500", description = "Error interno al solicitar acceso")
    })
    @PostMapping("/dashboard/solicitar")
    public ResponseEntity<?> solicitarAccesoDashboard(
            @Parameter(description = "ID de usuario Supervisor solicitante (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            DashboardPermisoDTO dto = notificacionService.solicitarAccesoDashboard(usuarioId, authHeader);
            if ("ERROR".equalsIgnoreCase(dto.getEstado()) || "DENEGADO".equalsIgnoreCase(dto.getEstado())) {
                return ResponseEntity.badRequest().body(dto);
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error al solicitar acceso al Dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al solicitar acceso: " + e.getMessage()));
        }
    }

    @Operation(summary = "Resolver solicitud de acceso al Dashboard (Propietario)",
            description = "Ejecuta el stored procedure `sp_notificaciones_resolver_dashboard` para autorizar o rechazar el acceso de un supervisor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resolución aplicada correctamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado (no es Propietario)"),
            @ApiResponse(responseCode = "500", description = "Error interno al resolver solicitud")
    })
    @PostMapping("/dashboard/resolver")
    public ResponseEntity<?> resolverAccesoDashboard(
            @Valid @RequestBody ResolverDashboardRequestDTO request,
            @Parameter(description = "ID del usuario Propietario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            DashboardPermisoDTO dto = notificacionService.resolverAccesoDashboard(
                    request.getSupervisorId(), request.getAutorizar(), usuarioId, authHeader);
            if ("ERROR".equalsIgnoreCase(dto.getEstado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(dto);
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error al resolver acceso al Dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al resolver acceso: " + e.getMessage()));
        }
    }

    @Operation(summary = "Consultar estado de permiso del Dashboard",
            description = "Consulta en tiempo real si el supervisor tiene acceso concedido (`APROBADO`) o si su solicitud sigue pendiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de acceso consultado correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno al consultar estado")
    })
    @GetMapping("/dashboard/estado")
    public ResponseEntity<?> consultarEstadoDashboard(
            @Parameter(description = "ID del supervisor a consultar (opcional)")
            @RequestParam(required = false) Integer supervisorId,
            @Parameter(description = "ID del usuario en sesión (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            DashboardPermisoDTO dto = notificacionService.consultarEstadoDashboard(
                    supervisorId, usuarioId, authHeader);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error al consultar estado de dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar estado: " + e.getMessage()));
        }
    }

    @Operation(summary = "Listar configuración de alertas de vencimiento por categoría",
            description = "Consulta la configuración actual de días de anticipación y lotes afectados para cada categoría de producto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error al consultar configuración de vencimientos")
    })
    @GetMapping("/vencimientos/configuracion")
    public ResponseEntity<?> listarConfiguracionVencimiento() {
        try {
            return ResponseEntity.ok(notificacionService.listarConfiguracionVencimiento());
        } catch (Exception e) {
            log.error("Error al obtener configuración de vencimientos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener configuración: " + e.getMessage()));
        }
    }

    @Operation(summary = "Configurar días de anticipación de vencimiento de forma global",
            description = "Aplica los días de anticipación a todas las categorías de producto activas simultáneamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración global actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PutMapping("/vencimientos/global")
    public ResponseEntity<?> actualizarVencimientoGlobal(
            @Valid @RequestBody ActualizarVencimientoGlobalRequestDTO request,
            @Parameter(description = "ID explícito de usuario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Map<String, Object> res = notificacionService.actualizarVencimientoGlobal(
                    request.getDiasAnticipacion(), usuarioId, authHeader);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Error al actualizar vencimiento global: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar configuración global: " + e.getMessage()));
        }
    }

    @Operation(summary = "Configurar días de anticipación de vencimiento de forma individual",
            description = "Actualiza los días de anticipación para una o más categorías de producto específicas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración individual actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PutMapping("/vencimientos/categoria")
    public ResponseEntity<?> actualizarVencimientoIndividual(
            @Valid @RequestBody ActualizarVencimientoIndividualRequestDTO request,
            @Parameter(description = "ID explícito de usuario (opcional si hay JWT)")
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Map<String, Object> res = notificacionService.actualizarVencimientoIndividual(
                    request, usuarioId, authHeader);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al actualizar vencimiento individual: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar configuración individual: " + e.getMessage()));
        }
    }
}
