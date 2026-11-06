package com.almacen.warnes.modulo_inventario.controller;

import com.almacen.warnes.modulo_inventario.dto.ProductoDTO;
import com.almacen.warnes.modulo_inventario.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Productos (Catálogo)", description = "Consultas maestras de productos activos en catálogo comercial")
@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @Operation(summary = "Listar productos activos", description = "Retorna todos los productos del catálogo comercial que no han sido dados de baja")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de productos activos obtenido exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarProductosActivos() {
        log.info("Consultando catálogo de productos activos");
        List<ProductoDTO> productos = productoService.listarProductosActivos();
        log.info("Total de productos activos recuperados: {}", productos.size());
        return ResponseEntity.ok(productos);
    }

    @Operation(summary = "Obtener producto por ID", description = "Recupera un producto específico por su identificador único")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado o dado de baja")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerPorId(
            @Parameter(description = "Identificador único del producto", example = "1", required = true)
            @PathVariable Integer id
    ) {
        log.info("Consultando producto con ID: {}", id);
        ProductoDTO dto = productoService.obtenerPorId(id);
        if (dto == null) {
            log.warn("Producto con ID {} no encontrado", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
