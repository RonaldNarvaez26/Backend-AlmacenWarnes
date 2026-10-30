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
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    // --- Endpoints de Consulta / Proyecciones (Paso 1) ---

    @Operation(
            summary = "Generar código interno correlativo",
            description = "Ejecuta la función PostgreSQL `spGenerarCodigoInterno(p_prefijo)`. Genera un código de 6 caracteres con formato XXX001 correlativo según el prefijo de la categoría."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código interno generado exitosamente",
                    content = @Content(schema = @Schema(implementation = CodigoInternoDTO.class)))
    })
    @GetMapping("/codigo-interno")
    public ResponseEntity<CodigoInternoDTO> generarCodigoInterno(
            @Parameter(description = "Prefijo alfabético de 3 letras de la categoría", example = "PRO")
            @RequestParam(name = "prefijo", defaultValue = "PRO") String prefijo
    ) {
        CodigoInternoDTO dto = inventarioService.obtenerCodigoInternoDTO(prefijo);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Obtener resumen de métricas clave (KPIs) de inventario",
            description = "Ejecuta la función almacenada `spInventarioKpiResumen()`. Calcula en tiempo real: productos en catálogo, unidades en stock activo, lotes en sala de espera y alertas de caducidad."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPIs calculados exitosamente",
                    content = @Content(schema = @Schema(implementation = InventarioKpiDTO.class)))
    })
    @GetMapping("/kpis")
    public ResponseEntity<InventarioKpiDTO> obtenerKpis() {
        InventarioKpiDTO kpis = inventarioService.obtenerKpiResumen();
        return ResponseEntity.ok(kpis);
    }

    @Operation(
            summary = "Listar inventario activo por lotes",
            description = "Ejecuta la función almacenada `spInventarioLotesListar()`. Retorna todo el stock físico detallado por lote, producto y distribuidor, con días para vencer y estado de oferta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de lotes obtenido exitosamente")
    })
    @GetMapping("/lotes")
    public ResponseEntity<List<InventarioLoteDTO>> listarLotes() {
        List<InventarioLoteDTO> lotes = inventarioService.listarLotes();
        return ResponseEntity.ok(lotes);
    }

    @Operation(
            summary = "Listar catálogo maestro con stock acumulado",
            description = "Ejecuta la función almacenada `spProductosListarCatalogo()`. Consolida el stock total disponible de cada producto sumando todas las existencias de sus lotes activos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo maestro recuperado exitosamente")
    })
    @GetMapping("/catalogo")
    public ResponseEntity<List<ProductoCatalogoDTO>> listarCatalogo() {
        List<ProductoCatalogoDTO> catalogo = inventarioService.listarCatalogo();
        return ResponseEntity.ok(catalogo);
    }

    // --- Endpoints de Sala de Espera (Paso 2) ---

    @Operation(
            summary = "Listar registros pendientes en Sala de Espera",
            description = "Consulta `inventario_pendientes` combinando en tránsito los datos de lotes y proveedores mediante consulta optimizada con JOIN sin alterar el esquema físico de la base de datos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mercadería en espera de auditoría")
    })
    @GetMapping("/pendientes")
    public ResponseEntity<List<InventarioPendienteDTO>> listarPendientes() {
        List<InventarioPendienteDTO> lista = inventarioService.listarPendientesActivos();
        return ResponseEntity.ok(lista);
    }

    @Operation(
            summary = "Registrar nuevo ingreso a Sala de Espera (Cajero)",
            description = "Inserta un registro con estado = 1 (Pendiente) en la tabla `inventario_pendientes`. No afecta el stock físico comercial hasta que el supervisor lo apruebe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro en sala de espera creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de validación erróneos")
    })
    @PostMapping("/pendientes")
    public ResponseEntity<InventarioPendienteDTO> registrarPendiente(@Valid @RequestBody RegistroPendienteRequestDTO dto) {
        InventarioPendienteDTO creado = inventarioService.registrarPendiente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
            summary = "Actualizar datos de mercadería en Sala de Espera",
            description = "Permite al cajero o supervisor corregir datos del registro pendiente antes de someterlo al proceso final de aprobación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro pendiente actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Registro pendiente no encontrado")
    })
    @PutMapping("/pendientes/{id}")
    public ResponseEntity<InventarioPendienteDTO> actualizarPendiente(
            @Parameter(description = "ID del registro en inventario_pendientes", example = "101", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody RegistroPendienteRequestDTO dto
    ) {
        InventarioPendienteDTO actualizado = inventarioService.actualizarPendiente(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(
            summary = "Aprobar ingreso de mercadería (Supervisor)",
            description = "Invoca el procedimiento almacenado `CALL spInventarioPendientesAprobar(?, ?, ?, ?, ?, ?)`. Asienta físicamente el lote en `lotes`, actualiza o crea el producto en `productos`, suma las existencias en `inventario` y marca el registro como aprobado (estado = 2)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote aprobado e ingresado al inventario exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación o el registro ya no se encuentra en estado pendiente"),
            @ApiResponse(responseCode = "404", description = "Registro pendiente no encontrado")
    })
    @PutMapping("/pendientes/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarPendiente(
            @Parameter(description = "ID del registro pendiente", example = "101", required = true)
            @PathVariable Integer id,
            @RequestBody(required = false) RegistroPendienteRequestDTO cambios,
            @Parameter(description = "ID del supervisor auditor", example = "2")
            @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision
    ) {
        inventarioService.aprobarPendiente(id, cambios, usuarioRevision);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Lote aprobado e ingresado al inventario exitosamente");
        response.put("inventarioPendienteId", id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Rechazar ingreso de mercadería (Supervisor)",
            description = "Marca el registro pendiente con estado = 3 (Rechazado). La mercadería no ingresa al stock físico comercial y queda asentada para auditoría."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro pendiente rechazado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El registro ya fue procesado previamente")
    })
    @PutMapping("/pendientes/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarPendiente(
            @Parameter(description = "ID del registro pendiente", example = "101", required = true)
            @PathVariable Integer id,
            @Parameter(description = "ID del supervisor auditor", example = "2")
            @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision
    ) {
        inventarioService.rechazarPendiente(id, usuarioRevision);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Registro pendiente rechazado exitosamente");
        response.put("inventarioPendienteId", id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Ingreso directo de mercadería por Supervisor",
            description = "Invoca el procedimiento almacenado transaccional `CALL spInventarioIngresoDirectoSupervisor(...)`. Permite al supervisor asentar producto, lote e inventario en una única transacción sin pasar por sala de espera."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingreso directo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de lote o producto inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto: Código de barras duplicado en otro producto")
    })
    @PostMapping("/ingreso-directo")
    public ResponseEntity<Map<String, String>> ingresoDirectoSupervisor(@Valid @RequestBody IngresoDirectoRequestDTO dto) {
        inventarioService.ingresoDirectoSupervisor(dto);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Ingreso directo de mercaderia registrado exitosamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- Operaciones de Soporte Frontend Vue 3 (Paso 3) ---

    @Operation(
            summary = "Buscar producto por código interno o código de barras",
            description = "Permite identificar rápidamente si un producto ya existe en catálogo al escanear con la pistola lectora o ingresar su código interno."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/productos/buscar")
    public ResponseEntity<?> buscarProductoPorCodigo(
            @Parameter(description = "Código de barras o código interno de 6 caracteres", example = "777123456001", required = true)
            @RequestParam(name = "codigo") String codigo
    ) {
        ProductoDTO producto = inventarioService.buscarProductoPorCodigo(codigo);
        if (producto == null) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("mensaje", "Producto no encontrado con el codigo especificado: " + codigo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(producto);
    }

    @Operation(
            summary = "Actualizar precio de oferta / descuento en un lote",
            description = "Modifica las columnas `en_descuento` y `precio_descuento` en la tabla `inventario` para promover la rotación rápida de lotes próximos a caducar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descuento de lote actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Lote de inventario no encontrado")
    })
    @PatchMapping("/lotes/{inventarioId}/descuento")
    public ResponseEntity<Map<String, Object>> actualizarDescuentoLote(
            @Parameter(description = "Identificador del registro en inventario", example = "1", required = true)
            @PathVariable Integer inventarioId,
            @Valid @RequestBody ActualizarDescuentoRequestDTO dto
    ) {
        inventarioService.actualizarDescuentoLote(inventarioId, dto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Descuento de lote actualizado exitosamente");
        response.put("inventarioId", inventarioId);
        response.put("enDescuento", dto.getEnDescuento());
        response.put("precioDescuento", dto.getPrecioDescuento());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Actualizar estado comercial de un producto en catálogo",
            description = "Modifica el estado en `productos`: 1 (Activo), 2 (Descontinuado), 3 (Suspendido)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del producto actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PatchMapping("/productos/{productoId}/estado")
    public ResponseEntity<Map<String, Object>> actualizarEstadoProducto(
            @Parameter(description = "Identificador del producto", example = "1", required = true)
            @PathVariable Integer productoId,
            @Valid @RequestBody ActualizarEstadoProductoRequestDTO dto
    ) {
        ProductoDTO actualizado = inventarioService.actualizarEstadoProducto(productoId, dto.getEstado());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Estado del producto actualizado exitosamente");
        response.put("productoId", productoId);
        response.put("estado", actualizado.getEstado());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Dar de baja lógica a un producto de catálogo",
            description = "Establece `marca_baja = true` en la tabla `productos` sin borrar físicamente el historial."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/productos/{productoId}")
    public ResponseEntity<Map<String, Object>> eliminarProducto(
            @Parameter(description = "Identificador del producto", example = "1", required = true)
            @PathVariable Integer productoId
    ) {
        inventarioService.eliminarLogicoProducto(productoId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Producto dado de baja exitosamente");
        response.put("productoId", productoId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Dar de baja lógica a un lote de inventario",
            description = "Establece `marca_baja = true` en la tabla `inventario` retirando el lote del stock vendible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "Lote no encontrado")
    })
    @DeleteMapping("/lotes/{inventarioId}")
    public ResponseEntity<Map<String, Object>> eliminarLote(
            @Parameter(description = "Identificador del inventario", example = "1", required = true)
            @PathVariable Integer inventarioId
    ) {
        inventarioService.eliminarLogicoLoteInventario(inventarioId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Lote de inventario dado de baja exitosamente");
        response.put("inventarioId", inventarioId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar proveedores activos (Dropdown auxiliar)",
            description = "Retorna la lista de proveedores activos para poblar selectores en formularios de ingreso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de proveedores activos")
    })
    @GetMapping("/proveedores")
    public ResponseEntity<List<ProveedorDTO>> listarProveedores() {
        List<ProveedorDTO> lista = inventarioService.listarProveedoresActivos();
        return ResponseEntity.ok(lista);
    }

    @Operation(
            summary = "Listar categorías activas (Dropdown auxiliar)",
            description = "Retorna las categorías comerciales activas desde la tabla paramétrica de Conceptos (Grupo 2)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de categorías activas")
    })
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> listarCategorias() {
        List<CategoriaDTO> lista = inventarioService.listarCategoriasActivas();
        return ResponseEntity.ok(lista);
    }
}
