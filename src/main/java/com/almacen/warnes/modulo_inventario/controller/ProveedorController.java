package com.almacen.warnes.modulo_inventario.controller;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.service.ProveedorService;
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

@Tag(name = "Proveedores y Distribuidores", description = "Gestión de distribuidores comerciales, acuerdos y contratos vigentes, catálogo abastecido y trazabilidad de lotes con caducidad")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @Operation(
            summary = "Obtener resumen de indicadores clave (KPIs)",
            description = "Ejecuta la función almacenada `spProveedorKpiResumen()` en PostgreSQL para calcular métricas cuantitativas: total de proveedores activos, proveedores con lotes en riesgo de caducidad (<= 7 días), total de productos distintos abastecidos, lotes activos y valorización total en inventario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPIs calculados exitosamente",
                    content = @Content(schema = @Schema(implementation = ProveedorKpiDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno en la ejecución de la función de base de datos")
    })
    @GetMapping("/kpis")
    public ResponseEntity<ProveedorKpiDTO> obtenerKpis() {
        ProveedorKpiDTO kpis = proveedorService.obtenerKpiResumen();
        return ResponseEntity.ok(kpis);
    }

    @Operation(
            summary = "Listar distribuidores con filtros combinados",
            description = "Ejecuta la función almacenada `spProveedorListar(p_busqueda, p_producto)`. Permite realizar búsqueda por razón social, contacto o contrato, o cruzar relacionalmente buscando por el nombre o código del producto que el proveedor suministra."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de distribuidores obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<ProveedorListadoDTO>> listarProveedores(
            @Parameter(description = "Texto para filtrar por nombre de empresa, contacto logístico o notas de contrato", example = "PIL Andina")
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @Parameter(description = "Texto para filtrar proveedores que suministran un producto específico (por nombre, código interno o código de barras)", example = "Leche")
            @RequestParam(name = "producto", required = false) String producto
    ) {
        List<ProveedorListadoDTO> proveedores = proveedorService.listarProveedores(busqueda, producto);
        return ResponseEntity.ok(proveedores);
    }

    @Operation(
            summary = "Obtener datos de un distribuidor por ID",
            description = "Recupera la entidad básica de un proveedor activo por su identificador único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidor encontrado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado o dado de baja")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> obtenerPorId(
            @Parameter(description = "Identificador único del distribuidor", example = "1", required = true)
            @PathVariable("id") Integer id
    ) {
        ProveedorDTO dto = proveedorService.obtenerPorId(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Registrar un nuevo distribuidor",
            description = "Invoca la función de PostgreSQL `spProveedorRegistrar(...)`. Valida obligatoriedad de campos y comprueba que no exista otro proveedor activo con la misma razón social (lanza `RAISE EXCEPTION` HTTP 409 en caso de colisión)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Distribuidor registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o campos obligatorios vacíos"),
            @ApiResponse(responseCode = "409", description = "Conflicto: Ya existe un distribuidor activo con el mismo nombre comercial")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarProveedor(@Valid @RequestBody CrearProveedorRequestDTO request) {
        Integer nuevoId = proveedorService.registrarProveedor(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor registrado exitosamente");
        response.put("proveedorId", nuevoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Actualizar datos y contrato del distribuidor",
            description = "Ejecuta el procedimiento almacenado `CALL spProveedorActualizar(...)`. Modifica razón social, teléfono, contacto y acuerdos contractuales (anotaciones) respetando el esquema físico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidor actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de validación erróneos"),
            @ApiResponse(responseCode = "404", description = "Distribuidor no existe o está dado de baja"),
            @ApiResponse(responseCode = "409", description = "El nuevo nombre ya se encuentra en uso por otro distribuidor activo")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> actualizarProveedor(
            @Parameter(description = "ID del distribuidor a modificar", example = "1", required = true)
            @PathVariable("id") Integer id,
            @Valid @RequestBody ActualizarProveedorRequestDTO request
    ) {
        proveedorService.actualizarProveedor(id, request);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor actualizado exitosamente");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Dar de baja lógica a un distribuidor",
            description = "Invoca el procedimiento almacenado `CALL spProveedorEliminar(...)` para establecer `marca_baja = TRUE`. Preserva intacto el historial de lotes y stock previo para trazabilidad contable y auditoría."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidor dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "El proveedor no existe o ya fue dado de baja previamente")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarProveedor(
            @Parameter(description = "ID del distribuidor a dar de baja", example = "1", required = true)
            @PathVariable("id") Integer id
    ) {
        proveedorService.eliminarProveedor(id);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", "Distribuidor dado de baja exitosamente");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Consultar catálogo de productos abastecidos por un proveedor",
            description = "Ejecuta la función almacenada `spProveedorCatalogoProductos(:proveedorId)`. Retorna los productos específicos que este distribuidor ha suministrado, con precios de compra, venta, estado y stock actual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo del proveedor recuperado con éxito")
    })
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProveedorCatalogoProductoDTO>> listarCatalogoProductos(
            @Parameter(description = "ID del proveedor a consultar", example = "1", required = true)
            @PathVariable("id") Integer id
    ) {
        List<ProveedorCatalogoProductoDTO> catalogo = proveedorService.listarCatalogoProductos(id);
        return ResponseEntity.ok(catalogo);
    }

    @Operation(
            summary = "Consultar historial de lotes y estado de caducidad por proveedor",
            description = "Ejecuta la función `spProveedorHistorialLotes(:proveedorId, :filtro)`. Permite auditar todos los lotes entregados por el proveedor, cálculo de días restantes para vencer y semáforo de caducidad."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de lotes obtenido exitosamente")
    })
    @GetMapping("/{id}/lotes")
    public ResponseEntity<List<ProveedorHistorialLoteDTO>> listarHistorialLotes(
            @Parameter(description = "ID del distribuidor", example = "1", required = true)
            @PathVariable("id") Integer id,
            @Parameter(description = "Filtro de estado: 'todos', 'activos', 'proximos_vencer', 'vencidos'", example = "todos")
            @RequestParam(name = "filtro", defaultValue = "todos") String filtro
    ) {
        List<ProveedorHistorialLoteDTO> lotes = proveedorService.listarHistorialLotes(id, filtro);
        return ResponseEntity.ok(lotes);
    }
}
