package com.almacen.warnes.modulo_inventario.controller;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.service.ProveedorService;
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
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    /**
     * GET /api/proveedores/kpis
     * Retorna indicadores clave de rendimiento del módulo de proveedores
     */
    @GetMapping("/kpis")
    public ResponseEntity<ProveedorKpiDTO> obtenerKpis() {
        ProveedorKpiDTO kpis = proveedorService.obtenerKpiResumen();
        return ResponseEntity.ok(kpis);
    }

    /**
     * GET /api/proveedores?busqueda={texto}&producto={texto}
     * Lista distribuidores con filtros combinados (empresa/contacto o producto abastecido)
     */
    @GetMapping
    public ResponseEntity<List<ProveedorListadoDTO>> listarProveedores(
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "producto", required = false) String producto
    ) {
        List<ProveedorListadoDTO> proveedores = proveedorService.listarProveedores(busqueda, producto);
        return ResponseEntity.ok(proveedores);
    }

    /**
     * GET /api/proveedores/{id}
     * Obtiene los datos de un distribuidor específico
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> obtenerPorId(@PathVariable("id") Integer id) {
        ProveedorDTO dto = proveedorService.obtenerPorId(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/proveedores
     * Registra un nuevo distribuidor ejecutando el SP spProveedorRegistrar
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarProveedor(@Valid @RequestBody CrearProveedorRequestDTO request) {
        Integer nuevoId = proveedorService.registrarProveedor(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor registrado exitosamente");
        response.put("proveedorId", nuevoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/proveedores/{id}
     * Actualiza un distribuidor y sus acuerdos contractuales mediante spProveedorActualizar
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> actualizarProveedor(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ActualizarProveedorRequestDTO request
    ) {
        proveedorService.actualizarProveedor(id, request);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor actualizado exitosamente");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/proveedores/{id}
     * Baja lógica de un distribuidor mediante spProveedorEliminar
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarProveedor(@PathVariable("id") Integer id) {
        proveedorService.eliminarProveedor(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor dado de baja exitosamente");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/proveedores/{id}/productos
     * Retorna el catálogo exacto de productos abastecidos por este distribuidor
     */
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProveedorCatalogoProductoDTO>> listarCatalogoProductos(@PathVariable("id") Integer id) {
        List<ProveedorCatalogoProductoDTO> catalogo = proveedorService.listarCatalogoProductos(id);
        return ResponseEntity.ok(catalogo);
    }

    /**
     * GET /api/proveedores/{id}/lotes?filtro={activos|proximos_vencer|vencidos|todos}
     * Retorna el historial de lotes recibidos y estado de caducidad
     */
    @GetMapping("/{id}/lotes")
    public ResponseEntity<List<ProveedorHistorialLoteDTO>> listarHistorialLotes(
            @PathVariable("id") Integer id,
            @RequestParam(name = "filtro", defaultValue = "todos") String filtro
    ) {
        List<ProveedorHistorialLoteDTO> lotes = proveedorService.listarHistorialLotes(id, filtro);
        return ResponseEntity.ok(lotes);
    }
}

