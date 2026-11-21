package com.almacen.warnes.modulo_ventas.controller;

import com.almacen.warnes.modulo_ventas.dto.*;
import com.almacen.warnes.modulo_ventas.service.VentaService;
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

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del módulo de Ventas POS.
 * Base: /api/ventas
 *
 * Endpoints:
 *   GET  /buscar-producto         → fn_pos_buscar_producto (buscador ágil POS)
 *   POST /registrar               → sp_ventas_registrar (registra venta + descuenta stock)
 *   POST /anular/{ventaId}        → sp_ventas_anular (anula venta + revierte stock)
 *   GET  /{ventaId}               → detalle de una venta
 *   GET  /historial               → listado de ventas por sucursal y fecha
 *   GET  /clientes/buscar         → buscar cliente por NIT/CI
 *   POST /clientes                → crear nuevo cliente
 */
@Tag(name = "Ventas POS", description = "Terminal POS: búsqueda de productos, registro de ventas, anulaciones, historial y gestión de clientes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    // =========================================================================
    // BUSCADOR POS
    // =========================================================================

    @Operation(
            summary = "Búsqueda ágil POS de productos",
            description = "Ejecuta fn_pos_buscar_producto(sucursal_id, criterio). " +
                          "El criterio puede ser código de barras (match exacto), código interno (match exacto) " +
                          "o parte del nombre (ILIKE). Retorna stock disponible y flag de descuento. " +
                          "Stock=0 debe ser manejado como Hard Stop en el frontend (no bloquea el endpoint)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos (puede ser lista vacía)"),
            @ApiResponse(responseCode = "400", description = "Criterio de búsqueda vacío o sucursalId inválido"),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar la función almacenada")
    })
    @GetMapping("/buscar-producto")
    public ResponseEntity<?> buscarProducto(
            @Parameter(description = "ID de la sucursal activa del cajero", example = "1")
            @RequestParam Integer sucursalId,
            @Parameter(description = "Código de barras, código interno o nombre parcial del producto", example = "LEC001")
            @RequestParam String criterio) {

        log.info("GET /api/ventas/buscar-producto → sucursal={} criterio='{}'", sucursalId, criterio);
        try {
            List<ProductoPOSDTO> resultados = ventaService.buscarProducto(sucursalId, criterio);
            return ResponseEntity.ok(resultados);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Búsqueda POS denegada: {}", rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error en búsqueda POS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al buscar producto: " + e.getMessage()));
        }
    }

    // =========================================================================
    // REGISTRAR VENTA
    // =========================================================================

    @Operation(
            summary = "Registrar venta y descontar stock (atómico)",
            description = "Ejecuta sp_ventas_registrar en una sola transacción. " +
                          "El SP valida que cobros mixtos sumen al total, bloquea filas de Inventario (FOR UPDATE) " +
                          "para evitar vender la última unidad dos veces, descuenta el stock y registra Detalles_Ventas. " +
                          "Retorna VentaResponseDTO con el HTML del recibo térmico 80mm listo para window.print(). " +
                          "NOTA ARQUITECTURAL: El frontend envía este request solo cuando expiran los 5 segundos " +
                          "de cuenta regresiva sin que el cajero presione Abortar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente, cobros no cuadran o datos inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar el procedimiento almacenado")
    })
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarVenta(@Valid @RequestBody RegistrarVentaDTO dto) {
        log.info("POST /api/ventas/registrar → cajero={} sucursal={} total={}",
                dto.getUsuarioId(), dto.getSucursalId(), dto.getPrecioTotal());
        try {
            VentaResponseDTO respuesta = ventaService.registrarVenta(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Venta rechazada: {}", rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al registrar venta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar venta: " + e.getMessage()));
        }
    }

    // =========================================================================
    // ANULAR VENTA
    // =========================================================================

    @Operation(
            summary = "Anular venta y revertir stock",
            description = "Ejecuta sp_ventas_anular. Cambia el estado de la venta a 3 (Anulada) " +
                          "y revierte el stock a cada lote de Inventario involucrado. " +
                          "El SP valida que la venta no esté ya anulada. " +
                          "Requiere rol Propietario (rol_id=3) o Supervisor (rol_id=2)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta anulada y stock revertido exitosamente"),
            @ApiResponse(responseCode = "400", description = "La venta ya está anulada o no existe"),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar el procedimiento almacenado")
    })
    @PostMapping("/anular/{ventaId}")
    public ResponseEntity<?> anularVenta(
            @Parameter(description = "ID de la venta a anular", example = "1234")
            @PathVariable Integer ventaId,
            @Valid @RequestBody AnularVentaDTO dto) {

        dto.setVentaId(ventaId); // asegurar consistencia
        log.info("POST /api/ventas/anular/{} → usuario={}", ventaId, dto.getUsuarioAnulacion());
        try {
            ventaService.anularVenta(dto);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Venta #" + ventaId + " anulada exitosamente. Stock revertido.",
                    "ventaId", ventaId
            ));
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Anulación denegada venta #{}: {}", ventaId, rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al anular venta #{}: {}", ventaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al anular venta: " + e.getMessage()));
        }
    }

    // =========================================================================
    // CONSULTAR VENTA
    // =========================================================================

    @Operation(
            summary = "Obtener detalle completo de una venta",
            description = "Retorna la cabecera y líneas de la venta junto con el recibo HTML regenerado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la venta obtenido"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/{ventaId}")
    public ResponseEntity<?> obtenerVenta(
            @Parameter(description = "ID de la venta", example = "1234")
            @PathVariable Integer ventaId) {

        log.info("GET /api/ventas/{}", ventaId);
        try {
            return ResponseEntity.ok(ventaService.obtenerVenta(ventaId));
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al obtener venta #{}: {}", ventaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar venta: " + e.getMessage()));
        }
    }

    // =========================================================================
    // HISTORIAL
    // =========================================================================

    @Operation(
            summary = "Historial de ventas por sucursal (sp_ventas_listar)",
            description = "Ejecuta la función almacenada sp_ventas_listar(p_sucursal_id, p_fecha, p_metodo_venta, p_estado). " +
                          "Retorna las ventas con sus nombres de cajero, sucursal, cliente, estado y el desglose de productos en JSON. " +
                          "Fecha opcional en formato yyyy-MM-dd."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente mediante procedimiento almacenado"),
            @ApiResponse(responseCode = "400", description = "sucursalId inválido o formato de fecha incorrecto"),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar sp_ventas_listar")
    })
    @GetMapping({"/historial", ""})
    public ResponseEntity<?> historial(
            @Parameter(description = "ID de la sucursal", example = "1")
            @RequestParam Integer sucursalId,
            @Parameter(description = "Fecha a filtrar en formato yyyy-MM-dd (opcional — omitir para todas)", example = "2026-11-16")
            @RequestParam(required = false) String fecha,
            @Parameter(description = "ID del método de venta: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto (opcional)", example = "1")
            @RequestParam(required = false) Integer metodoVenta,
            @Parameter(description = "ID del estado de la venta: 1=Pendiente, 2=Finalizada, 3=Anulada (opcional)", example = "2")
            @RequestParam(required = false) Integer estado) {

        log.info("GET /api/ventas/historial → sucursal={} fecha='{}' metodo={} estado={}",
                sucursalId, fecha != null ? fecha : "todas", metodoVenta, estado);
        try {
            return ResponseEntity.ok(ventaService.listarVentas(sucursalId, fecha, metodoVenta, estado));
        } catch (Exception e) {
            log.error("Error al listar ventas via sp_ventas_listar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar historial: " + e.getMessage()));
        }
    }

    // =========================================================================
    // CLIENTES
    // =========================================================================

    @Operation(
            summary = "Buscar cliente por NIT/CI",
            description = "Busca en la tabla Clientes por el campo nit (varchar 20). " +
                          "Retorna null (HTTP 200 con body null) si no existe — el frontend puede crear uno nuevo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado o null si no existe"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/clientes/buscar")
    public ResponseEntity<?> buscarCliente(
            @Parameter(description = "NIT o CI del cliente", example = "12345678")
            @RequestParam String nit) {

        log.info("GET /api/ventas/clientes/buscar → nit='{}'", nit);
        try {
            ClienteDTO cliente = ventaService.buscarCliente(nit);
            return ResponseEntity.ok(cliente); // null si no existe
        } catch (Exception e) {
            log.error("Error al buscar cliente NIT='{}': {}", nit, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al buscar cliente: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Crear nuevo cliente",
            description = "Inserta en la tabla Clientes. Si ya existe un cliente activo con el mismo NIT, " +
                          "retorna el existente sin crear duplicados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado o cliente existente retornado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/clientes")
    public ResponseEntity<?> crearCliente(
            @Valid @RequestBody ClienteDTO dto,
            @Parameter(description = "ID del usuario que crea el registro (para auditoría)", example = "3")
            @RequestParam(required = false, defaultValue = "1") Integer usuarioCreacion) {

        log.info("POST /api/ventas/clientes → NIT='{}'", dto.getNit());
        try {
            ClienteDTO resultado = ventaService.crearCliente(dto, usuarioCreacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al crear cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear cliente: " + e.getMessage()));
        }
    }
}
