package com.almacen.warnes.modulo_inventario.controller;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.service.InventarioService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Inventario y Sala de Espera", description = "Operaciones de stock físico, lotes por caducidad, auditoría en Sala de Espera y catálogo maestro de productos")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

        private final InventarioService inventarioService;
        private final com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper securityContextHelper;

        // --- Endpoints de Consulta / Proyecciones (Paso 1) ---

        @Operation(summary = "Generar código interno correlativo", description = "Ejecuta la función PostgreSQL `spGenerarCodigoInterno(p_prefijo)`. Genera un código de 6 caracteres con formato XXX001 correlativo según el prefijo de la categoría.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Código interno generado exitosamente", content = @Content(schema = @Schema(implementation = CodigoInternoDTO.class)))
        })
        @GetMapping("/codigo-interno")
        public ResponseEntity<CodigoInternoDTO> generarCodigoInterno(
                        @Parameter(description = "Prefijo alfabético de 3 letras de la categoría", example = "PRO") @RequestParam(name = "prefijo", defaultValue = "PRO") String prefijo) {
                log.info("Generando código interno correlativo para prefijo '{}'", prefijo);
                CodigoInternoDTO dto = inventarioService.obtenerCodigoInternoDTO(prefijo);
                log.info("Código interno correlativo generado: '{}'", dto.getCodigoInterno());
                return ResponseEntity.ok(dto);
        }

        @Operation(summary = "Obtener resumen de métricas clave (KPIs) de inventario", description = "Ejecuta la función almacenada `spInventarioKpiResumen(:sucursalId)`. Calcula en tiempo real: productos en catálogo, unidades en stock activo, lotes en sala de espera y alertas de caducidad, filtrable por sucursal.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "KPIs calculados exitosamente", content = @Content(schema = @Schema(implementation = InventarioKpiDTO.class)))
        })
        @GetMapping("/kpis")
        public ResponseEntity<InventarioKpiDTO> obtenerKpis(
                        @RequestParam(name = "sucursalId", required = false) Integer sucursalId,
                        @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                Integer sucursalFinal = securityContextHelper.resolverSucursalFiltro(sucursalId, usuarioId, authHeader);
                log.info("Consultando KPIs de resumen de inventario (sucursalId: {})", sucursalFinal);
                InventarioKpiDTO kpis = inventarioService.obtenerKpiResumen(sucursalFinal);
                log.info("KPIs calculados -> Catálogo: {}, Stock Activo: {}, Sala Espera: {}, Alertas Caducidad: {}",
                                kpis.getTotalProductosCatalogo(), kpis.getUnidadesStockActivo(),
                                kpis.getLotesSalaEspera(), kpis.getAlertasVencimiento());
                return ResponseEntity.ok(kpis);
        }

        @Operation(summary = "Listar inventario activo por lotes", description = "Ejecuta la función almacenada `spInventarioLotesListar(:estado)`. Retorna el stock físico detallado por lote, producto y distribuidor, filtrado por estado de registro (ACTIVOS, INACTIVOS, TODOS).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de lotes obtenido exitosamente")
        })
        @GetMapping("/lotes")
        public ResponseEntity<List<InventarioLoteDTO>> listarLotes(
                        @RequestParam(name = "estado", required = false, defaultValue = "ACTIVOS") String estado,
                        @RequestParam(name = "estadoRegistro", required = false) String estadoRegistro,
                        @RequestParam(name = "marcaBaja", required = false) Boolean marcaBaja,
                        @RequestParam(name = "sucursalId", required = false) Integer sucursalId,
                        @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                String estadoFinal = resolverEstadoFiltro(estado, estadoRegistro, marcaBaja);
                Integer sucursalFinal = securityContextHelper.resolverSucursalFiltro(sucursalId, usuarioId, authHeader);
                log.info("Consultando listado de inventario activo por lotes (filtro: '{}', sucursalId: {})", estadoFinal, sucursalFinal);
                List<InventarioLoteDTO> lotes = inventarioService.listarLotes(estadoFinal, sucursalFinal);
                log.info("Total de lotes de inventario recuperados: {}", lotes.size());
                return ResponseEntity.ok(lotes);
        }

        @Operation(summary = "Listar catálogo maestro con stock acumulado", description = "Ejecuta la función almacenada `spProductosListarCatalogo(:estado)`. Consolida el stock total disponible de cada producto sumando todas las existencias de sus lotes, filtrado por estado de registro.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Catálogo maestro recuperado exitosamente")
        })
        @GetMapping("/catalogo")
        public ResponseEntity<List<ProductoCatalogoDTO>> listarCatalogo(
                        @RequestParam(name = "estado", required = false, defaultValue = "ACTIVOS") String estado,
                        @RequestParam(name = "estadoRegistro", required = false) String estadoRegistro,
                        @RequestParam(name = "marcaBaja", required = false) Boolean marcaBaja,
                        @RequestParam(name = "sucursalId", required = false) Integer sucursalId,
                        @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                String estadoFinal = resolverEstadoFiltro(estado, estadoRegistro, marcaBaja);
                Integer sucursalFinal = securityContextHelper.resolverSucursalFiltro(sucursalId, usuarioId, authHeader);
                log.info("Consultando catálogo maestro con stock acumulado (filtro: '{}', sucursalId: {})", estadoFinal, sucursalFinal);
                List<ProductoCatalogoDTO> catalogo = inventarioService.listarCatalogo(estadoFinal, sucursalFinal);
                log.info("Catálogo maestro cargado con {} productos registrados", catalogo.size());
                return ResponseEntity.ok(catalogo);
        }

        private String resolverEstadoFiltro(String estado, String estadoRegistro, Boolean marcaBaja) {
                if (Boolean.TRUE.equals(marcaBaja))
                        return "INACTIVOS";
                if (Boolean.FALSE.equals(marcaBaja))
                        return "ACTIVOS";
                if (estadoRegistro != null && !estadoRegistro.trim().isEmpty()) {
                        String er = estadoRegistro.trim().toUpperCase();
                        if (er.contains("BAJA") || er.contains("INACTIVO"))
                                return "INACTIVOS";
                        if (er.contains("TODO"))
                                return "TODOS";
                        if (er.contains("ACTIVO"))
                                return "ACTIVOS";
                        return er;
                }
                if (estado != null && !estado.trim().isEmpty()) {
                        return estado.trim().toUpperCase();
                }
                return "ACTIVOS";
        }

        @Operation(summary = "Listar sucursales activas", description = "Retorna el listado de sucursales físicas activas del sistema para el selector de sucursal.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de sucursales obtenido exitosamente")
        })
        @GetMapping("/sucursales")
        public ResponseEntity<List<SucursalDTO>> listarSucursales() {
                log.info("Consultando listado de sucursales activas");
                List<SucursalDTO> sucursales = inventarioService.listarSucursales();
                return ResponseEntity.ok(sucursales);
        }

        // --- Endpoints de Sala de Espera (Paso 2) ---

        @Operation(summary = "Listar registros pendientes en Sala de Espera", description = "Consulta `inventario_pendientes` combinando en tránsito los datos de lotes y proveedores mediante consulta optimizada con JOIN sin alterar el esquema físico de la base de datos.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lista de mercadería en espera de auditoría")
        })
        @GetMapping("/pendientes")
        public ResponseEntity<List<InventarioPendienteDTO>> listarPendientes(
                        @RequestParam(name = "sucursalId", required = false) Integer sucursalId,
                        @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                Integer sucursalFinal = securityContextHelper.resolverSucursalFiltro(sucursalId, usuarioId, authHeader);
                log.info("Consultando registros pendientes en Sala de Espera (sucursalId: {})", sucursalFinal);
                List<InventarioPendienteDTO> lista = inventarioService.listarPendientesActivos(sucursalFinal);
                log.info("Total de registros en Sala de Espera recuperados: {}", lista.size());
                return ResponseEntity.ok(lista);
        }

        @Operation(summary = "Registrar nuevo ingreso a Sala de Espera (Cajero)", description = "Inserta un registro con estado = 1 (Pendiente) en la tabla `inventario_pendientes`. No afecta el stock físico comercial hasta que el supervisor lo apruebe.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Registro en sala de espera creado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de validación erróneos")
        })
        @PostMapping("/pendientes")
        public ResponseEntity<InventarioPendienteDTO> registrarPendiente(
                        @Valid @RequestBody RegistroPendienteRequestDTO dto,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                Integer sucursalOp = securityContextHelper.resolverSucursalOperacion(dto.getSucursalId(), dto.getUsuarioCreacion(), authHeader);
                dto.setSucursalId(sucursalOp);
                log.info("Registrando ingreso a Sala de Espera -> Producto: '{}', Stock: {}, Proveedor ID: {}, Sucursal ID: {}",
                                dto.getNombreProducto(), dto.getStockIngresado(), dto.getProveedorId(),
                                dto.getSucursalId());
                InventarioPendienteDTO creado = inventarioService.registrarPendiente(dto);
                log.info("Registro creado en Sala de Espera exitosamente con ID: {}",
                                creado.getInventarioPendienteId());
                return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        }

        @Operation(summary = "Actualizar datos de mercadería en Sala de Espera", description = "Permite al cajero o supervisor corregir datos del registro pendiente antes de someterlo al proceso final de aprobación.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro pendiente actualizado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Registro pendiente no encontrado")
        })
        @PutMapping("/pendientes/{id}")
        public ResponseEntity<InventarioPendienteDTO> actualizarPendiente(
                        @Parameter(description = "ID del registro en inventario_pendientes", example = "101", required = true) @PathVariable Integer id,
                        @Valid @RequestBody RegistroPendienteRequestDTO dto) {
                log.info("Actualizando mercadería en Sala de Espera ID: {}", id);
                InventarioPendienteDTO actualizado = inventarioService.actualizarPendiente(id, dto);
                log.info("Registro ID: {} en Sala de Espera actualizado exitosamente", id);
                return ResponseEntity.ok(actualizado);
        }

        @Operation(summary = "Aprobar ingreso de mercadería (Supervisor)", description = "Invoca el procedimiento almacenado `CALL spInventarioPendientesAprobar(?, ?, ?, ?, ?, ?)`. Asienta físicamente el lote en `lotes`, actualiza o crea el producto en `productos`, suma las existencias en `inventario` y marca el registro como aprobado (estado = 2).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lote aprobado e ingresado al inventario exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Error en validación o el registro ya no se encuentra en estado pendiente"),
                        @ApiResponse(responseCode = "404", description = "Registro pendiente no encontrado")
        })
        @PutMapping("/pendientes/{id}/aprobar")
        public ResponseEntity<?> aprobarPendiente(
                        @Parameter(description = "ID del registro pendiente", example = "101", required = true) @PathVariable Integer id,
                        @RequestBody(required = false) RegistroPendienteRequestDTO cambios,
                        @Parameter(description = "ID del supervisor auditor", example = "2") @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                if (!securityContextHelper.tienePermisoEscritura(usuarioRevision, authHeader)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("mensaje", "Acceso denegado: El rol Cajero no tiene permisos para aprobar mercadería en Sala de Espera"));
                }
                log.info("Supervisor ID: {} iniciando aprobación de mercadería en Sala de Espera ID: {}",
                                usuarioRevision, id);
                inventarioService.aprobarPendiente(id, cambios, usuarioRevision);
                log.info("Mercadería ID: {} aprobada e ingresada exitosamente al inventario físico", id);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Lote aprobado e ingresado al inventario exitosamente");
                response.put("inventarioPendienteId", id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Rechazar ingreso de mercadería (Supervisor)", description = "Marca el registro pendiente con estado = 3 (Rechazado). La mercadería no ingresa al stock físico comercial y queda asentada para auditoría.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro pendiente rechazado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "El registro ya fue procesado previamente")
        })
        @PutMapping("/pendientes/{id}/rechazar")
        public ResponseEntity<?> rechazarPendiente(
                        @Parameter(description = "ID del registro pendiente", example = "101", required = true) @PathVariable Integer id,
                        @Parameter(description = "Motivo del rechazo", example = "Mercadería dañada o empaque roto") @RequestParam(name = "motivo", required = false) String motivo,
                        @Parameter(description = "ID del supervisor auditor", example = "2") @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                if (!securityContextHelper.tienePermisoEscritura(usuarioRevision, authHeader)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("mensaje", "Acceso denegado: El rol Cajero no tiene permisos para rechazar mercadería en Sala de Espera"));
                }
                log.info("Supervisor ID: {} rechazando ingreso en Sala de Espera ID: {}, motivo: '{}'", usuarioRevision,
                                id, motivo);
                inventarioService.rechazarPendiente(id, motivo, usuarioRevision);
                log.info("Ingreso ID: {} marcado como rechazado exitosamente", id);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Registro pendiente rechazado exitosamente");
                response.put("inventarioPendienteId", id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Mandar a corregir ingreso de mercadería (Supervisor)", description = "Devuelve el registro pendiente al cajero con estado = 4 (Observado / Para Corrección). Guarda la observación para que el cajero la visualice y rectifique los datos.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro devuelto al cajero para corrección exitosamente"),
                        @ApiResponse(responseCode = "400", description = "El registro no se encuentra en estado modificable"),
                        @ApiResponse(responseCode = "404", description = "Registro pendiente no encontrado")
        })
        @PutMapping("/pendientes/{id}/observar")
        public ResponseEntity<?> observarPendiente(
                        @Parameter(description = "ID del registro pendiente", example = "101", required = true) @PathVariable Integer id,
                        @Parameter(description = "Instrucción u observación de corrección para el cajero", example = "Precio de compra inconsistente con factura, verificar por favor") @RequestParam(name = "observacion", required = false) String observacion,
                        @Parameter(description = "ID del supervisor auditor", example = "2") @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                if (!securityContextHelper.tienePermisoEscritura(usuarioRevision, authHeader)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("mensaje", "Acceso denegado: El rol Cajero no tiene permisos para observar mercadería en Sala de Espera"));
                }
                log.info("Supervisor ID: {} observando mercadería en Sala de Espera ID: {}, observación: '{}'",
                                usuarioRevision, id, observacion);
                inventarioService.observarPendiente(id, observacion, usuarioRevision);
                log.info("Registro ID: {} devuelto al cajero con estado Observado", id);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Registro devuelto al cajero para corrección exitosamente");
                response.put("inventarioPendienteId", id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Ingreso directo de mercadería por Supervisor", description = "Invoca el procedimiento almacenado transaccional `CALL spInventarioIngresoDirectoSupervisor(...)`. Permite al supervisor asentar producto, lote e inventario en una única transacción sin pasar por sala de espera.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Ingreso directo registrado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de lote o producto inválidos"),
                        @ApiResponse(responseCode = "409", description = "Conflicto: Código de barras duplicado en otro producto")
        })
        @PostMapping("/ingreso-directo")
        public ResponseEntity<?> ingresoDirectoSupervisor(
                        @Valid @RequestBody IngresoDirectoRequestDTO dto,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                if (!securityContextHelper.tienePermisoEscritura(dto.getUsuarioCreacion(), authHeader)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("mensaje", "Acceso denegado: El rol Cajero no tiene permisos para realizar ingreso directo"));
                }
                Integer sucursalOp = securityContextHelper.resolverSucursalOperacion(dto.getSucursalId(), dto.getUsuarioCreacion(), authHeader);
                dto.setSucursalId(sucursalOp);
                log.info("Supervisor registrando ingreso directo -> Producto: '{}', Categoría: {}, Cantidad: {}, Costo Lote: {}, Sucursal: {}",
                                dto.getNombreProducto(), dto.getCategoriaId(), dto.getStockIngresado(),
                                dto.getPrecioLote(), dto.getSucursalId());
                inventarioService.ingresoDirectoSupervisor(dto);
                log.info("Ingreso directo asentado exitosamente en catálogo, lotes e inventario");
                Map<String, String> response = new LinkedHashMap<>();
                response.put("mensaje", "Ingreso directo de mercaderia registrado exitosamente");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // --- Operaciones de Soporte Frontend Vue 3 (Paso 3) ---

        @Operation(summary = "Buscar producto por código interno o código de barras", description = "Permite identificar rápidamente si un producto ya existe en catálogo al escanear con la pistola lectora o ingresar su código interno.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @GetMapping("/productos/buscar")
        public ResponseEntity<?> buscarProductoPorCodigo(
                        @Parameter(description = "Código de barras o código interno de 6 caracteres", example = "777123456001") @RequestParam(name = "codigo", required = false) String codigo,
                        @RequestParam(name = "codigoBarras", required = false) String codigoBarras) {
                String codigoFinal = (codigoBarras != null && !codigoBarras.trim().isEmpty()) ? codigoBarras : codigo;
                if (codigoFinal == null || codigoFinal.trim().isEmpty()) {
                        Map<String, String> error = new LinkedHashMap<>();
                        error.put("mensaje", "Debe proporcionar el parámetro 'codigo' o 'codigoBarras'");
                        return ResponseEntity.badRequest().body(error);
                }
                ProductoDTO producto = inventarioService.buscarProductoPorCodigo(codigoFinal);
                if (producto == null) {
                        log.warn("Producto no encontrado con el código: '{}'", codigoFinal);
                        Map<String, String> error = new LinkedHashMap<>();
                        error.put("mensaje", "Producto no encontrado con el codigo especificado: " + codigoFinal);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
                log.info("Producto encontrado -> ID: {}, Nombre: '{}', Precio Venta: {}",
                                producto.getProductoId(), producto.getNombreProducto(), producto.getPrecioVenta());
                return ResponseEntity.ok(producto);
        }

        @Operation(summary = "Actualizar precio de oferta / descuento en un lote", description = "Modifica las columnas `en_descuento` y `precio_descuento` en la tabla `inventario` para promover la rotación rápida de lotes próximos a caducar.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Descuento de lote actualizado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Lote de inventario no encontrado")
        })
        @PatchMapping("/lotes/{inventarioId}/descuento")
        public ResponseEntity<Map<String, Object>> actualizarDescuentoLote(
                        @Parameter(description = "Identificador del registro en inventario", example = "1", required = true) @PathVariable Integer inventarioId,
                        @Valid @RequestBody ActualizarDescuentoRequestDTO dto) {
                log.info("Actualizando descuento para lote de inventario ID: {}, enDescuento: {}, precioDescuento: {}",
                                inventarioId, dto.getEnDescuento(), dto.getPrecioDescuento());
                inventarioService.actualizarDescuentoLote(inventarioId, dto);
                log.info("Descuento para lote ID: {} actualizado exitosamente", inventarioId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Descuento de lote actualizado exitosamente");
                response.put("inventarioId", inventarioId);
                response.put("enDescuento", dto.getEnDescuento());
                response.put("precioDescuento", dto.getPrecioDescuento());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Actualizar estado comercial de un producto en catálogo", description = "Modifica el estado en `productos`: 1 (Activo), 2 (Descontinuado), 3 (Suspendido).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Estado del producto actualizado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @PatchMapping("/productos/{productoId}/estado")
        public ResponseEntity<Map<String, Object>> actualizarEstadoProducto(
                        @Parameter(description = "Identificador del producto", example = "1", required = true) @PathVariable Integer productoId,
                        @Valid @RequestBody ActualizarEstadoProductoRequestDTO dto) {
                log.info("Actualizando estado comercial del producto ID: {} a nuevo estado: {}", productoId,
                                dto.getEstado());
                ProductoDTO actualizado = inventarioService.actualizarEstadoProducto(productoId, dto.getEstado());
                log.info("Estado del producto ID: {} actualizado exitosamente a {}", productoId,
                                actualizado.getEstado());
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Estado del producto actualizado exitosamente");
                response.put("productoId", productoId);
                response.put("estado", actualizado.getEstado());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Dar de baja lógica a un producto de catálogo", description = "Establece `marca_baja = true` en la tabla `productos` sin borrar físicamente el historial.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Producto dado de baja exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
        })
        @DeleteMapping("/productos/{productoId}")
        public ResponseEntity<Map<String, Object>> eliminarProducto(
                        @Parameter(description = "Identificador del producto", example = "1", required = true) @PathVariable Integer productoId) {
                log.info("Dando de baja lógica a producto de catálogo ID: {}", productoId);
                inventarioService.eliminarLogicoProducto(productoId);
                log.info("Producto ID: {} dado de baja lógica exitosamente", productoId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Producto dado de baja exitosamente");
                response.put("productoId", productoId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Reactivar o restaurar producto dado de baja en catálogo")
        @PutMapping("/productos/{productoId}/reactivar")
        public ResponseEntity<Map<String, Object>> reactivarProducto(
                        @Parameter(description = "Identificador del producto a reactivar", example = "1", required = true) @PathVariable Integer productoId,
                        @RequestParam(name = "usuarioModificacion", required = false) Integer usuarioModificacion) {
                log.info("Reactivando producto de catálogo ID: {} por usuario ID: {}", productoId,
                                usuarioModificacion != null ? usuarioModificacion : 1);
                inventarioService.reactivarProducto(productoId, usuarioModificacion != null ? usuarioModificacion : 1);
                log.info("Producto ID: {} reactivado exitosamente en catálogo", productoId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Producto reactivado exitosamente en catálogo");
                response.put("productoId", productoId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Dar de baja lógica a un lote de inventario", description = "Establece `marca_baja = true` en la tabla `inventario` retirando el lote del stock vendible.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lote dado de baja exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Lote no encontrado")
        })
        @DeleteMapping("/lotes/{inventarioId}")
        public ResponseEntity<Map<String, Object>> eliminarLote(
                        @Parameter(description = "Identificador del inventario", example = "1", required = true) @PathVariable Integer inventarioId) {
                log.info("Dando de baja lógica a lote de inventario ID: {}", inventarioId);
                inventarioService.eliminarLogicoLoteInventario(inventarioId);
                log.info("Lote de inventario ID: {} dado de baja lógica exitosamente", inventarioId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Lote de inventario dado de baja exitosamente");
                response.put("inventarioId", inventarioId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Reactivar o restaurar lote de inventario dado de baja")
        @PutMapping("/lotes/{inventarioId}/reactivar")
        public ResponseEntity<Map<String, Object>> reactivarLote(
                        @Parameter(description = "Identificador del registro de inventario a reactivar", example = "1", required = true) @PathVariable Integer inventarioId,
                        @RequestParam(name = "usuarioModificacion", required = false) Integer usuarioModificacion) {
                log.info("Reactivando lote de inventario ID: {} por usuario ID: {}", inventarioId,
                                usuarioModificacion != null ? usuarioModificacion : 1);
                inventarioService.reactivarLoteInventario(inventarioId,
                                usuarioModificacion != null ? usuarioModificacion : 1);
                log.info("Lote de inventario ID: {} reactivado exitosamente", inventarioId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Lote de inventario reactivado exitosamente");
                response.put("inventarioId", inventarioId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Descontar unidades específicas de stock (Merma / Ajuste / Rotura)", description = "Ejecuta el procedimiento almacenado `CALL spInventarioDescontarUnidades(...)` para retirar unidades físicas de un lote sin dar de baja el producto general del catálogo. Si la cantidad a restar supera el stock disponible, la base de datos lanza una excepción controlada.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Unidades descontadas exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Cantidad a descontar inválida o supera el stock disponible"),
                        @ApiResponse(responseCode = "404", description = "Registro de inventario no encontrado o dado de baja")
        })
        @PutMapping({ "/{id}/descontar-unidades", "/lotes/{id}/descontar-unidades" })
        public ResponseEntity<Map<String, Object>> descontarUnidades(
                        @Parameter(description = "Identificador del registro en inventario", example = "1", required = true) @PathVariable("id") Integer id,
                        @Valid @RequestBody DescontarUnidadesRequestDTO request) {
                log.info("Descontando {} unidades de lote ID: {}, motivo: '{}', usuario ID: {}",
                                request.getCantidad(), id, request.getMotivo(), request.getUsuarioId());
                inventarioService.descontarUnidades(id, request);
                log.info("Se descontaron {} unidades del lote ID: {} exitosamente", request.getCantidad(), id);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "Se descontaron " + request.getCantidad() + " unidades de stock exitosamente");
                response.put("inventarioId", id);
                response.put("cantidadDescontada", request.getCantidad());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Listar proveedores activos (Dropdown auxiliar)", description = "Retorna la lista de proveedores activos para poblar selectores en formularios de ingreso.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de proveedores activos")
        })
        @GetMapping("/proveedores")
        public ResponseEntity<List<ProveedorDTO>> listarProveedores(
                        @Parameter(description = "ID de la sucursal para filtrar proveedores (opcional para Propietarios)") @RequestParam(name = "sucursalId", required = false) Integer sucursalId,
                        @Parameter(description = "ID del usuario que realiza la consulta") @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
                        @RequestHeader(name = "Authorization", required = false) String authHeader) {
                Integer sucursalFiltro = securityContextHelper.resolverSucursalFiltro(sucursalId, usuarioId, authHeader);
                log.info("Consultando lista auxiliar de proveedores activos para selectores de inventario -> sucursal: {}", sucursalFiltro);
                List<ProveedorDTO> lista = inventarioService.listarProveedoresActivos(sucursalFiltro);
                log.info("Total de proveedores activos para selector: {}", lista.size());
                return ResponseEntity.ok(lista);
        }

        @Operation(summary = "Listar categorías activas (Dropdown auxiliar)", description = "Retorna las categorías comerciales activas desde la tabla paramétrica de Conceptos (Grupo 2).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de categorías activas")
        })
        @GetMapping("/categorias")
        public ResponseEntity<List<CategoriaDTO>> listarCategorias() {
                log.info("Consultando lista auxiliar de categorías activas");
                List<CategoriaDTO> lista = inventarioService.listarCategoriasActivas();
                log.info("Total de categorías activas recuperadas: {}", lista.size());
                return ResponseEntity.ok(lista);
        }

        @Operation(summary = "Registrar nueva categoría de producto (Grupo 2)", description = "Inserta un nuevo concepto en la tabla paramétrica `conceptos` (grupo_id = 2) con el siguiente código secuencial disponible y retorna el listado actualizado.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente y lista actualizada retornada"),
                        @ApiResponse(responseCode = "400", description = "Nombre de categoría inválido o ya existente")
        })
        @PostMapping("/categorias")
        public ResponseEntity<List<CategoriaDTO>> crearCategoria(
                        @Valid @RequestBody CrearCategoriaRequestDTO request) {
                log.info("Registrando nueva categoría de producto: '{}'", request.getNombre());
                List<CategoriaDTO> lista = inventarioService.crearCategoria(request);
                log.info("Categoría '{}' registrada exitosamente, total categorías: {}", request.getNombre(), lista.size());
                return ResponseEntity.status(HttpStatus.CREATED).body(lista);
        }

        @Operation(summary = "Listar todas las categorías (Gestión administrativa)", description = "Retorna todas las categorías del grupo 2 de conceptos incluyendo activas y dadas de baja.")
        @GetMapping("/categorias/gestion")
        public ResponseEntity<List<CategoriaDTO>> listarTodasCategorias() {
                log.info("Consultando listado completo de categorías para gestión administrativa");
                List<CategoriaDTO> lista = inventarioService.listarTodasCategorias();
                log.info("Total de categorías en gestión administrativa: {}", lista.size());
                return ResponseEntity.ok(lista);
        }

        @Operation(summary = "Actualizar nombre de una categoría", description = "Modifica la descripción de una categoría paramétrica (Grupo 2).")
        @PutMapping("/categorias/{codigo}")
        public ResponseEntity<CategoriaDTO> actualizarCategoria(
                        @PathVariable Integer codigo,
                        @RequestBody Map<String, String> body) {
                String nuevoNombre = body.get("nombre");
                log.info("Actualizando categoría código {}: nuevo nombre '{}'", codigo, nuevoNombre);
                CategoriaDTO dto = inventarioService.actualizarCategoria(codigo, nuevoNombre);
                log.info("Categoría código {} actualizada exitosamente", codigo);
                return ResponseEntity.ok(dto);
        }

        @Operation(summary = "Activar o dar de baja lógica a una categoría", description = "Establece marca_baja = true/false en conceptos grupo 2.")
        @PatchMapping("/categorias/{codigo}/estado")
        public ResponseEntity<CategoriaDTO> cambiarEstadoCategoria(
                        @PathVariable Integer codigo,
                        @RequestParam(name = "baja", defaultValue = "true") Boolean baja) {
                log.info("Modificando estado de categoría código {} a baja={}", codigo, baja);
                CategoriaDTO dto = inventarioService.cambiarEstadoCategoria(codigo, baja);
                log.info("Estado de categoría código {} actualizado exitosamente", codigo);
                return ResponseEntity.ok(dto);
        }
}
