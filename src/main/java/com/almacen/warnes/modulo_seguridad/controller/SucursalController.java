package com.almacen.warnes.modulo_seguridad.controller;

import com.almacen.warnes.modulo_seguridad.dto.CrearSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.ModificarSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.SucursalDTO;
import com.almacen.warnes.modulo_seguridad.service.SucursalService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Seguridad - Sucursales", description = "Gestión integral de sucursales físicas (CRUD y listados con PostgreSQL)")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping({"/api/seguridad/sucursales", "/api/sucursales"})
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService sucursalService;

    @Operation(summary = "Listar sucursales con filtros dinámicos",
            description = "Ejecuta la función PostgreSQL `fn_sucursales_listar(p_mostrar_bajas, p_texto_busqueda)`. Retorna datos de sucursal y la cantidad de usuarios activos asignados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de sucursales obtenido exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar la función almacenada")
    })
    @GetMapping({"", "/listar-tabla"})
    public ResponseEntity<?> listarSucursales(
            @Parameter(description = "Indica si se incluyen sucursales con marca_baja = true", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean mostrarBajas,
            @Parameter(description = "Texto para buscar coincidencias en código, ciudad o dirección", example = "Central")
            @RequestParam(required = false) String textoBusqueda) {

        log.info("GET /api/sucursales/listar-tabla -> mostrarBajas: {}, textoBusqueda: '{}'",
                mostrarBajas, textoBusqueda != null ? textoBusqueda : "");

        try {
            List<SucursalDTO> listado = sucursalService.listarSucursales(mostrarBajas, textoBusqueda);
            return ResponseEntity.ok(listado);
        } catch (Exception e) {
            log.error("Error al listar sucursales: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar sucursales: " + e.getMessage()));
        }
    }

    @Operation(summary = "Crear nueva sucursal",
            description = "Ejecuta el procedimiento `sp_sucursales_insertar`. Requiere rol de Propietario (rol_id = 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sucursal creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o código interno duplicado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no es Propietario)"),
            @ApiResponse(responseCode = "500", description = "Error interno al crear sucursal")
    })
    @PostMapping
    public ResponseEntity<?> crearSucursal(
            @Valid @RequestBody CrearSucursalDTO dto,
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("POST /api/sucursales -> código: '{}', ciudad: '{}'", dto.getCodigoInterno(), dto.getCiudad());

        try {
            Integer nuevaSucursalId = sucursalService.crearSucursal(dto, usuarioId, authHeader);
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "Sucursal creada exitosamente.");
            resp.put("sucursalId", nuevaSucursalId);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Excepción de estado al crear sucursal: {}", rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al crear sucursal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al registrar sucursal"));
        }
    }

    @Operation(summary = "Modificar sucursal existente",
            description = "Ejecuta el procedimiento `sp_sucursales_modificar`. Requiere rol de Propietario (rol_id = 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal modificada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no es Propietario)"),
            @ApiResponse(responseCode = "500", description = "Error interno al modificar sucursal")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarSucursal(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ModificarSucursalDTO dto,
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("PUT /api/sucursales/{} -> código: '{}', ciudad: '{}'", id, dto.getCodigoInterno(), dto.getCiudad());

        try {
            sucursalService.modificarSucursal(id, dto, usuarioId, authHeader);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal modificada exitosamente.", "sucursalId", id));
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Excepción de estado al modificar sucursal: {}", rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al modificar sucursal ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al actualizar sucursal"));
        }
    }

    @Operation(summary = "Dar de baja lógica a una sucursal",
            description = "Ejecuta el procedimiento `sp_sucursales_eliminar`. Requiere rol de Propietario (rol_id = 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal dada de baja exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no es Propietario)"),
            @ApiResponse(responseCode = "500", description = "Error interno al dar de baja sucursal")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSucursal(
            @PathVariable("id") Integer id,
            @RequestParam(required = false) Integer usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("DELETE /api/sucursales/{}", id);

        try {
            sucursalService.eliminarSucursal(id, usuarioId, authHeader);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal dada de baja exitosamente.", "sucursalId", id));
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("Excepción de estado al dar de baja sucursal: {}", rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
        } catch (Exception e) {
            log.error("Error al dar de baja sucursal ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la baja de sucursal: " + e.getMessage()));
        }
    }
}

