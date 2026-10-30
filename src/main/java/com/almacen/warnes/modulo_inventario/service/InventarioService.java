package com.almacen.warnes.modulo_inventario.service;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProductoCatalogoProjection;
import com.almacen.warnes.modulo_inventario.entity.Inventario;
import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import com.almacen.warnes.modulo_inventario.mapper.InventarioMapper;
import com.almacen.warnes.modulo_inventario.mapper.ProductoMapper;
import com.almacen.warnes.modulo_inventario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final InventarioPendienteRepository inventarioPendienteRepository;
    private final InventarioProcedureRepository inventarioProcedureRepository;
    private final ProveedorRepository proveedorRepository;
    private final ConceptoRepository conceptoRepository;
    private final InventarioMapper inventarioMapper;
    private final ProductoMapper productoMapper;

    // --- Consultas / Proyecciones (Paso 1) ---

    public String generarCodigoInterno(String prefijo) {
        String p = (prefijo == null || prefijo.trim().isEmpty()) ? "PRO" : prefijo.trim().toUpperCase();
        return productoRepository.generarCodigoInterno(p);
    }

    public CodigoInternoDTO obtenerCodigoInternoDTO(String prefijo) {
        String p = (prefijo == null || prefijo.trim().isEmpty()) ? "PRO" : prefijo.trim().toUpperCase();
        String codigo = productoRepository.generarCodigoInterno(p);
        return CodigoInternoDTO.builder()
                .prefijo(p)
                .codigoInterno(codigo)
                .build();
    }

    public InventarioKpiDTO obtenerKpiResumen() {
        InventarioKpiProjection projection = inventarioRepository.obtenerKpiResumen();
        return inventarioMapper.toKpiDTO(projection);
    }

    public InventarioKpiProjection obtenerKpiResumenProyeccion() {
        return inventarioRepository.obtenerKpiResumen();
    }

    public List<InventarioLoteDTO> listarLotes() {
        return inventarioRepository.listarLotes().stream()
                .map(inventarioMapper::toLoteDTO)
                .collect(Collectors.toList());
    }

    public List<InventarioLoteProjection> listarLotesProyeccion() {
        return inventarioRepository.listarLotes();
    }

    public List<ProductoCatalogoDTO> listarCatalogo() {
        return productoRepository.listarCatalogo().stream()
                .map(inventarioMapper::toCatalogoDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoCatalogoProjection> listarCatalogoProyeccion() {
        return productoRepository.listarCatalogo();
    }

    // --- Sala de Espera (Paso 2) ---

    public List<InventarioPendienteDTO> listarPendientesActivos() {
        return inventarioPendienteRepository.findByEstadoAndMarcaBajaFalse(1).stream()
                .map(inventarioMapper::toPendienteDTO)
                .collect(Collectors.toList());
    }

    public InventarioPendienteDTO obtenerPendientePorId(Integer id) {
        return inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .map(inventarioMapper::toPendienteDTO)
                .orElse(null);
    }

    @Transactional
    public InventarioPendienteDTO registrarPendiente(RegistroPendienteRequestDTO dto) {
        InventarioPendiente entity = inventarioMapper.toPendienteEntity(dto);
        if (entity.getDescripcionLote() == null || entity.getDescripcionLote().trim().isEmpty()) {
            entity.setDescripcionLote("Lote " + (entity.getCodigoInterno() != null ? entity.getCodigoInterno() : "General"));
        }
        InventarioPendiente guardado = inventarioPendienteRepository.save(entity);
        return inventarioMapper.toPendienteDTO(guardado);
    }

    @Transactional
    public InventarioPendienteDTO actualizarPendiente(Integer id, RegistroPendienteRequestDTO dto) {
        // 1. Buscar la entidad por su ID (validando que no tenga marca de baja)
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro pendiente no encontrado con id: " + id));

        // Validar que se encuentre en espera (estado 1)
        if (pendiente.getEstado() != null && pendiente.getEstado() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden modificar registros pendientes en estado de espera (estado 1)");
        }

        // 2. Actualizar únicamente los campos editables
        if (dto.getNombreProducto() != null && !dto.getNombreProducto().trim().isEmpty()) {
            pendiente.setNombreProducto(dto.getNombreProducto().trim());
        }
        if (dto.getCodigoBarras() != null) {
            pendiente.setCodigoBarras(dto.getCodigoBarras().trim());
        }
        if (dto.getCategoriaId() != null) {
            pendiente.setCategoriaId(dto.getCategoriaId());
        }
        if (dto.getProveedorId() != null) {
            pendiente.setProveedorId(dto.getProveedorId());
        }
        if (dto.getStockIngresado() != null) {
            pendiente.setStockIngresado(dto.getStockIngresado());
        }
        if (dto.getPrecioCompra() != null) {
            pendiente.setPrecioCompra(dto.getPrecioCompra());
        }
        if (dto.getPrecioVenta() != null) {
            pendiente.setPrecioVenta(dto.getPrecioVenta());
        }
        if (dto.getPrecioLote() != null) {
            pendiente.setPrecioLote(dto.getPrecioLote());
        }
        if (dto.getFechaVencimiento() != null) {
            pendiente.setFechaVencimiento(dto.getFechaVencimiento());
        }
        if (dto.getDescripcionLote() != null && !dto.getDescripcionLote().trim().isEmpty()) {
            pendiente.setDescripcionLote(dto.getDescripcionLote().trim());
        }

        // 3. Crucial: Asegurarse de que el estado se mantenga intacto en 1 (Pendiente de Revisión)
        pendiente.setEstado(1);

        // 4. Guardar los cambios usando el método .save() estándar de JPA
        InventarioPendiente actualizado = inventarioPendienteRepository.save(pendiente);
        return inventarioMapper.toPendienteDTO(actualizado);
    }

    @Transactional
    public void aprobarPendiente(Integer id, RegistroPendienteRequestDTO cambios, Integer usuarioRevisionParam) {
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Registro pendiente no encontrado con id: " + id));

        if (pendiente.getEstado() != null && pendiente.getEstado() != 1) {
            throw new IllegalStateException("Solo se pueden aprobar registros en estado de espera (estado 1)");
        }

        if (cambios != null) {
            aplicarCambiosAPendiente(pendiente, cambios);
            inventarioPendienteRepository.saveAndFlush(pendiente);
        }

        Integer usuarioRevision = usuarioRevisionParam;
        if (usuarioRevision == null && cambios != null && cambios.getUsuarioRevision() != null) {
            usuarioRevision = cambios.getUsuarioRevision();
        }
        if (usuarioRevision == null) {
            usuarioRevision = 1;
        }

        inventarioProcedureRepository.ejecutarAprobarPendiente(id, usuarioRevision);
    }

    @Transactional
    public void rechazarPendiente(Integer id, Integer usuarioRevisionParam) {
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Registro pendiente no encontrado con id: " + id));

        if (pendiente.getEstado() != null && pendiente.getEstado() != 1) {
            throw new IllegalStateException("Solo se pueden rechazar registros en estado de espera (estado 1)");
        }

        Integer usuarioRevision = usuarioRevisionParam != null ? usuarioRevisionParam : 1;
        pendiente.setEstado(3); // 3 = Rechazado / Anulado
        pendiente.setFechaRevision(LocalDateTime.now());
        pendiente.setUsuarioRevision(usuarioRevision);

        inventarioPendienteRepository.save(pendiente);
    }

    @Transactional
    public void ingresoDirectoSupervisor(IngresoDirectoRequestDTO dto) {
        if (dto.getUsuarioCreacion() == null) {
            dto.setUsuarioCreacion(1);
        }
        inventarioProcedureRepository.ejecutarIngresoDirectoSupervisor(dto);
    }

    // --- Operaciones de Soporte Frontend Vue 3 (Paso 3) ---

    public ProductoDTO buscarProductoPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        return productoRepository.buscarPorCodigoBarrasOCodigoInterno(codigo.trim())
                .map(productoMapper::toDTO)
                .orElse(null);
    }

    @Transactional
    public void actualizarDescuentoLote(Integer inventarioId, ActualizarDescuentoRequestDTO dto) {
        Inventario inventario = inventarioRepository.findByInventarioIdAndSucursalIdAndMarcaBajaFalse(inventarioId, 1)
                .orElseThrow(() -> new IllegalArgumentException("Lote de inventario no encontrado o dado de baja con id: " + inventarioId));

        inventario.setEnDescuento(dto.getEnDescuento());
        inventario.setPrecioDescuento(dto.getPrecioDescuento());
        inventarioRepository.save(inventario);
    }

    @Transactional
    public ProductoDTO actualizarEstadoProducto(Integer productoId, Integer estado) {
        Producto producto = productoRepository.findByProductoIdAndMarcaBajaFalse(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o dado de baja con id: " + productoId));

        if (estado == null || estado < 1 || estado > 3) {
            throw new IllegalArgumentException("El estado debe ser 1 (Activo), 2 (Descontinuado) o 3 (Suspendido)");
        }

        producto.setEstado(estado);
        producto.setFechaModificacion(LocalDateTime.now());
        Producto actualizado = productoRepository.save(producto);
        return productoMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminarLogicoProducto(Integer productoId) {
        Producto producto = productoRepository.findByProductoIdAndMarcaBajaFalse(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o ya dado de baja con id: " + productoId));

        producto.setMarcaBaja(true);
        producto.setFechaModificacion(LocalDateTime.now());
        productoRepository.save(producto);
    }

    @Transactional
    public void eliminarLogicoLoteInventario(Integer inventarioId) {
        Inventario inventario = inventarioRepository.findByInventarioIdAndSucursalIdAndMarcaBajaFalse(inventarioId, 1)
                .orElseThrow(() -> new IllegalArgumentException("Lote de inventario no encontrado o ya dado de baja con id: " + inventarioId));

        inventario.setMarcaBaja(true);
        inventarioRepository.save(inventario);
    }

    public List<ProveedorDTO> listarProveedoresActivos() {
        return proveedorRepository.findByMarcaBajaFalseOrderByNombreEmpresaAsc().stream()
                .map(inventarioMapper::toProveedorDTO)
                .collect(Collectors.toList());
    }

    public List<CategoriaDTO> listarCategoriasActivas() {
        return conceptoRepository.findByGrupoIdAndMarcaBajaFalseOrderByCodigoAsc(2).stream()
                .map(inventarioMapper::toCategoriaDTO)
                .collect(Collectors.toList());
    }

    private void aplicarCambiosAPendiente(InventarioPendiente pendiente, RegistroPendienteRequestDTO dto) {
        if (dto.getNombreProducto() != null && !dto.getNombreProducto().trim().isEmpty()) {
            pendiente.setNombreProducto(dto.getNombreProducto().trim());
        }
        if (dto.getCodigoBarras() != null) {
            pendiente.setCodigoBarras(dto.getCodigoBarras().trim());
        }
        if (dto.getCodigoInterno() != null && !dto.getCodigoInterno().trim().isEmpty()) {
            pendiente.setCodigoInterno(dto.getCodigoInterno().trim());
        }
        if (dto.getCategoriaId() != null) {
            pendiente.setCategoriaId(dto.getCategoriaId());
        }
        if (dto.getPrecioCompra() != null) {
            pendiente.setPrecioCompra(dto.getPrecioCompra());
        }
        if (dto.getPrecioVenta() != null) {
            pendiente.setPrecioVenta(dto.getPrecioVenta());
        }
        if (dto.getStockIngresado() != null) {
            pendiente.setStockIngresado(dto.getStockIngresado());
        }
        if (dto.getProveedorId() != null) {
            pendiente.setProveedorId(dto.getProveedorId());
        }
        if (dto.getDescripcionLote() != null) {
            pendiente.setDescripcionLote(dto.getDescripcionLote().trim());
        }
        if (dto.getPrecioLote() != null) {
            pendiente.setPrecioLote(dto.getPrecioLote());
        }
        if (dto.getFechaVencimiento() != null) {
            pendiente.setFechaVencimiento(dto.getFechaVencimiento());
        }
        if (dto.getProductoId() != null) {
            pendiente.setProductoId(dto.getProductoId());
        }
    }
}
