package com.almacen.warnes.modulo_inventario.controller;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/codigo-interno")
    public ResponseEntity<CodigoInternoDTO> generarCodigoInterno(@RequestParam(name = "prefijo", defaultValue = "PRO") String prefijo) {
        CodigoInternoDTO dto = inventarioService.obtenerCodigoInternoDTO(prefijo);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/kpis")
    public ResponseEntity<InventarioKpiDTO> obtenerKpis() {
        InventarioKpiDTO kpis = inventarioService.obtenerKpiResumen();
        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/lotes")
    public ResponseEntity<List<InventarioLoteDTO>> listarLotes() {
        List<InventarioLoteDTO> lotes = inventarioService.listarLotes();
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/catalogo")
    public ResponseEntity<List<ProductoCatalogoDTO>> listarCatalogo() {
        List<ProductoCatalogoDTO> catalogo = inventarioService.listarCatalogo();
        return ResponseEntity.ok(catalogo);
    }

    // --- Endpoints de Sala de Espera (Paso 2) ---

    @GetMapping("/pendientes")
    public ResponseEntity<List<InventarioPendienteDTO>> listarPendientes() {
        List<InventarioPendienteDTO> lista = inventarioService.listarPendientesActivos();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/pendientes")
    public ResponseEntity<InventarioPendienteDTO> registrarPendiente(@Valid @RequestBody RegistroPendienteRequestDTO dto) {
        InventarioPendienteDTO creado = inventarioService.registrarPendiente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/pendientes/{id}")
    public ResponseEntity<InventarioPendienteDTO> actualizarPendiente(
            @PathVariable Integer id,
            @Valid @RequestBody RegistroPendienteRequestDTO dto) {
        InventarioPendienteDTO actualizado = inventarioService.actualizarPendiente(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/pendientes/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarPendiente(
            @PathVariable Integer id,
            @RequestBody(required = false) RegistroPendienteRequestDTO cambios,
            @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision) {
        inventarioService.aprobarPendiente(id, cambios, usuarioRevision);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Lote aprobado e ingresado al inventario exitosamente");
        response.put("inventarioPendienteId", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/pendientes/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarPendiente(
            @PathVariable Integer id,
            @RequestParam(name = "usuarioRevision", required = false) Integer usuarioRevision) {
        inventarioService.rechazarPendiente(id, usuarioRevision);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Registro pendiente rechazado exitosamente");
        response.put("inventarioPendienteId", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingreso-directo")
    public ResponseEntity<Map<String, String>> ingresoDirectoSupervisor(@Valid @RequestBody IngresoDirectoRequestDTO dto) {
        inventarioService.ingresoDirectoSupervisor(dto);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Ingreso directo de mercaderia registrado exitosamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- Operaciones de Soporte Frontend Vue 3 (Paso 3) ---

    @GetMapping("/productos/buscar")
    public ResponseEntity<?> buscarProductoPorCodigo(@RequestParam(name = "codigo") String codigo) {
        ProductoDTO producto = inventarioService.buscarProductoPorCodigo(codigo);
        if (producto == null) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("mensaje", "Producto no encontrado con el codigo especificado: " + codigo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(producto);
    }

    @PatchMapping("/lotes/{inventarioId}/descuento")
    public ResponseEntity<Map<String, Object>> actualizarDescuentoLote(
            @PathVariable Integer inventarioId,
            @Valid @RequestBody ActualizarDescuentoRequestDTO dto) {
        inventarioService.actualizarDescuentoLote(inventarioId, dto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Descuento de lote actualizado exitosamente");
        response.put("inventarioId", inventarioId);
        response.put("enDescuento", dto.getEnDescuento());
        response.put("precioDescuento", dto.getPrecioDescuento());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/productos/{productoId}/estado")
    public ResponseEntity<Map<String, Object>> actualizarEstadoProducto(
            @PathVariable Integer productoId,
            @Valid @RequestBody ActualizarEstadoProductoRequestDTO dto) {
        ProductoDTO actualizado = inventarioService.actualizarEstadoProducto(productoId, dto.getEstado());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Estado del producto actualizado exitosamente");
        response.put("productoId", productoId);
        response.put("estado", actualizado.getEstado());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/productos/{productoId}")
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable Integer productoId) {
        inventarioService.eliminarLogicoProducto(productoId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Producto dado de baja exitosamente");
        response.put("productoId", productoId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/lotes/{inventarioId}")
    public ResponseEntity<Map<String, Object>> eliminarLote(@PathVariable Integer inventarioId) {
        inventarioService.eliminarLogicoLoteInventario(inventarioId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Lote de inventario dado de baja exitosamente");
        response.put("inventarioId", inventarioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proveedores")
    public ResponseEntity<List<ProveedorDTO>> listarProveedores() {
        List<ProveedorDTO> lista = inventarioService.listarProveedoresActivos();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> listarCategorias() {
        List<CategoriaDTO> lista = inventarioService.listarCategoriasActivas();
        return ResponseEntity.ok(lista);
    }
}
