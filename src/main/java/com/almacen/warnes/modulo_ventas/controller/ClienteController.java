package com.almacen.warnes.modulo_ventas.controller;

import com.almacen.warnes.modulo_ventas.dto.ClienteDTO;
import com.almacen.warnes.modulo_ventas.service.ClienteService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el mantenimiento y consulta de Clientes (ABM).
 * Consume los Procedimientos Almacenados mediante ClienteService.
 */
@Slf4j
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión integral de clientes (ABM y búsqueda por NIT/CI)")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(
            summary = "Listar clientes con filtros",
            description = "Retorna el listado de clientes filtrando por búsqueda (NIT, nombre, razón social) y estado (ACTIVOS, BAJAS, TODOS). " +
                          "Incluye total acumulado de compras mediante sp_clientes_listar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de clientes obtenido exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listarClientes(
            @Parameter(description = "Texto de búsqueda (NIT, nombre, razón social)", example = "Juan")
            @RequestParam(required = false) String busqueda,
            @Parameter(description = "Filtro de estado: ACTIVOS, BAJAS, TODOS", example = "ACTIVOS")
            @RequestParam(required = false, defaultValue = "ACTIVOS") String estado) {

        log.info("GET /api/clientes → busqueda='{}', estado='{}'", busqueda, estado);
        List<ClienteDTO> clientes = clienteService.listarClientes(busqueda, estado);
        return ResponseEntity.ok(clientes);
    }

    @Operation(
            summary = "Buscar cliente por NIT o CI",
            description = "Ejecuta sp_clientes_buscar_nit para localizar de forma exacta un cliente activo en el POS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado o null"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorNit(
            @Parameter(description = "NIT o CI exacto del cliente", example = "12345678")
            @RequestParam String nit) {

        log.info("GET /api/clientes/buscar → nit='{}'", nit);
        try {
            ClienteDTO cliente = clienteService.buscarPorNit(nit);
            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            log.error("Error al buscar cliente por NIT: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al buscar cliente: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Obtener cliente por ID",
            description = "Retorna los datos de un cliente específico por su identificador primario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID del cliente", example = "1")
            @PathVariable Integer id) {

        log.info("GET /api/clientes/{}", id);
        ClienteDTO cliente = clienteService.obtenerPorId(id);
        if (cliente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente con ID " + id + " no encontrado"));
        }
        return ResponseEntity.ok(cliente);
    }

    @Operation(
            summary = "Registrar nuevo cliente",
            description = "Registra un cliente o reactiva/actualiza uno existente si ya tenía el mismo NIT (sp_clientes_insertar)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping
    public ResponseEntity<?> crearCliente(
            @Valid @RequestBody ClienteDTO dto,
            @Parameter(description = "ID del usuario que realiza la operación", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer usuarioCreacion) {

        log.info("POST /api/clientes → NIT='{}', Nombre='{}'", dto.getNit(), dto.getNombreRazonSocial());
        try {
            ClienteDTO nuevo = clienteService.crearCliente(dto, usuarioCreacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al registrar cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar cliente: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Modificar cliente existente",
            description = "Actualiza los datos del cliente mediante sp_clientes_modificar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o colisión de NIT"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarCliente(
            @Parameter(description = "ID del cliente a modificar", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ClienteDTO dto,
            @Parameter(description = "ID del usuario que realiza la modificación", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer usuarioModificacion) {

        log.info("PUT /api/clientes/{} → NIT='{}'", id, dto.getNit());
        try {
            dto.setClienteId(id);
            ClienteDTO actualizado = clienteService.modificarCliente(id, dto, usuarioModificacion);
            return ResponseEntity.ok(actualizado);
        } catch (ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al modificar cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al modificar cliente: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Dar de baja lógica a un cliente",
            description = "Marca al cliente con marca_baja = true mediante sp_clientes_eliminar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente dado de baja exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(
            @Parameter(description = "ID del cliente a dar de baja", example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID del usuario que realiza la baja", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer usuarioModificacion) {

        log.info("DELETE /api/clientes/{}", id);
        try {
            clienteService.eliminarCliente(id, usuarioModificacion);
            return ResponseEntity.ok(Map.of("mensaje", "Cliente #" + id + " dado de baja exitosamente"));
        } catch (Exception e) {
            log.error("Error al dar de baja al cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al dar de baja al cliente: " + e.getMessage()));
        }
    }
}
