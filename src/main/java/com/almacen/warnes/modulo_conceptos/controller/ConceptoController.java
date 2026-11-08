package com.almacen.warnes.modulo_conceptos.controller;

import com.almacen.warnes.modulo_conceptos.dto.ConceptoDTO;
import com.almacen.warnes.modulo_conceptos.dto.CrearConceptoDTO;
import com.almacen.warnes.modulo_conceptos.dto.ModificarConceptoDTO;
import com.almacen.warnes.modulo_conceptos.service.ConceptoService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Conceptos Paramétricos", description = "Consultas y administración de conceptos y catálogos paramétricos (Roles, Categorías, Estados, Métodos de Pago)")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@Slf4j
@RestController
@RequestMapping("/api/conceptos")
@RequiredArgsConstructor
public class ConceptoController {

    private final ConceptoService conceptoService;

    @Operation(summary = "Listar conceptos activos por grupo", description = "Ejecuta la función PostgreSQL `fn_conceptos_listar_por_grupo(p_grupo_id)`. Excluye la cabecera (código 0) y retorna conceptos activos ordenados por código.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de conceptos obtenido exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno en la base de datos")
    })
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ConceptoDTO>> listarPorGrupo(
            @Parameter(description = "ID del grupo paramétrico (1=Roles, 2=Categorías, 3=Estado Prod, 4=Métodos Pago, etc.)", example = "2", required = true)
            @PathVariable Integer grupoId
    ) {
        log.info("Consultando conceptos paramétricos para el grupo_id: {}", grupoId);
        List<ConceptoDTO> lista = conceptoService.listarPorGrupo(grupoId);
        log.info("Total conceptos recuperados para grupo {}: {}", grupoId, lista.size());
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Registrar nuevo concepto", description = "Invoca el procedimiento `sp_conceptos_insertar(...)`. Aplica validación RBAC por grupo para evitar que roles no autorizados creen conceptos estructurales (roles, estados de sistema).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Concepto registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o duplicados"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado para el grupo seleccionado")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearConcepto(
            @Valid @RequestBody CrearConceptoDTO dto,
            @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        log.info("Solicitud para registrar nuevo concepto en grupo {}: '{}'", dto.getGrupoId(), dto.getDescripcion());
        Integer nuevoId = conceptoService.insertarConcepto(dto, usuarioId, authHeader);
        log.info("Concepto registrado exitosamente con ID: {}", nuevoId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Concepto registrado exitosamente");
        response.put("conceptoId", nuevoId);
        response.put("grupoId", dto.getGrupoId());
        response.put("descripcion", dto.getDescripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modificar descripción de un concepto", description = "Invoca el procedimiento `sp_conceptos_modificar(...)`. Valida que el usuario tenga privilegios suficientes para alterar el grupo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Concepto modificado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o concepto no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado para modificar conceptos de este grupo")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modificarConcepto(
            @Parameter(description = "ID del concepto a modificar", example = "6", required = true)
            @PathVariable("id") Integer id,
            @Valid @RequestBody ModificarConceptoDTO dto,
            @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        log.info("Solicitud para modificar concepto ID {}: nueva descripción '{}'", id, dto.getDescripcion());
        conceptoService.modificarConcepto(id, dto, usuarioId, authHeader);
        log.info("Concepto ID {} modificado exitosamente", id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Concepto modificado exitosamente");
        response.put("conceptoId", id);
        response.put("descripcion", dto.getDescripcion());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar lógicamente un concepto", description = "Invoca el procedimiento `sp_conceptos_eliminar(...)`. Bloquea la eliminación de conceptos en grupos estructurales del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Concepto dado de baja exitosamente"),
            @ApiResponse(responseCode = "400", description = "Concepto no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: conceptos de grupos estructurales no se pueden eliminar")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarConcepto(
            @Parameter(description = "ID del concepto a dar de baja", example = "6", required = true)
            @PathVariable("id") Integer id,
            @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        log.info("Solicitud para eliminar lógicamente concepto ID: {}", id);
        conceptoService.eliminarConcepto(id, usuarioId, authHeader);
        log.info("Concepto ID {} dado de baja exitosamente", id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Concepto dado de baja exitosamente");
        response.put("conceptoId", id);
        return ResponseEntity.ok(response);
    }
}

