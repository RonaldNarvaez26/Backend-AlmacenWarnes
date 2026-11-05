package com.almacen.warnes.modulo_inventario.service;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioPendienteDetalleProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProductoCatalogoProjection;
import com.almacen.warnes.modulo_inventario.entity.Inventario;
import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import com.almacen.warnes.modulo_inventario.entity.Lote;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import com.almacen.warnes.modulo_inventario.mapper.InventarioMapper;
import com.almacen.warnes.modulo_inventario.mapper.ProductoMapper;
import com.almacen.warnes.modulo_inventario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    // Bitácora en memoria para observaciones de auditoría y corrección sin alterar el esquema físico
    private final Map<Integer, String> observacionesPendientes = new ConcurrentHashMap<>();

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final InventarioPendienteRepository inventarioPendienteRepository;
    private final InventarioProcedureRepository inventarioProcedureRepository;
    private final LoteRepository loteRepository;
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

    /**
     * Lista registros en espera combinando la tabla inventario_pendientes con
     * el último lote conocido del producto mediante @Query con JOIN, sin tocar el esquema de BD.
     */
    public List<InventarioPendienteDTO> listarPendientesActivos() {
        List<InventarioPendienteDetalleProjection> projections = inventarioPendienteRepository.findAllConDetalleLote();
        if (projections != null && !projections.isEmpty()) {
            return projections.stream().map(p -> {
                BigDecimal precioLote = (p.getPrecioCompra() != null && p.getStockIngresado() != null)
                        ? p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockIngresado()))
                        : null;
                return InventarioPendienteDTO.builder()
                        .inventarioPendienteId(p.getInventarioPendienteId())
                        .sucursalId(p.getSucursalId())
                        .productoId(p.getProductoId())
                        .nombreProducto(p.getNombreProducto())
                        .codigoBarras(p.getCodigoBarras())
                        .codigoInterno(p.getCodigoInterno())
                        .categoriaId(p.getCategoriaId())
                        .precioCompra(p.getPrecioCompra())
                        .precioVenta(p.getPrecioVenta())
                        .stockIngresado(p.getStockIngresado())
                        .estado(p.getEstado())
                        .marcaBaja(p.getMarcaBaja())
                        .fechaCreacion(p.getFechaCreacion())
                        .usuarioCreacion(p.getUsuarioCreacion())
                        .fechaRevision(p.getFechaRevision())
                        .usuarioRevision(p.getUsuarioRevision())
                        .proveedorId(p.getProveedorId())
                        .descripcionLote(p.getDescripcionLote() != null ? p.getDescripcionLote() : "Lote " + p.getCodigoInterno())
                        .precioLote(precioLote)
                        .fechaVencimiento(p.getFechaVencimiento())
                        .observacionRevision(observacionesPendientes.get(p.getInventarioPendienteId()))
                        .build();
            }).collect(Collectors.toList());
        }

        return inventarioPendienteRepository.findByMarcaBajaFalse().stream()
                .map(item -> {
                    InventarioPendienteDTO dto = inventarioMapper.toPendienteDTO(item);
                    dto.setObservacionRevision(observacionesPendientes.get(item.getInventarioPendienteId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public InventarioPendienteDTO obtenerPendientePorId(Integer id) {
        return inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .map(item -> {
                    InventarioPendienteDTO dto = inventarioMapper.toPendienteDTO(item);
                    dto.setObservacionRevision(observacionesPendientes.get(id));
                    return dto;
                })
                .orElse(null);
    }

    @Transactional
    public InventarioPendienteDTO registrarPendiente(RegistroPendienteRequestDTO dto) {
        InventarioPendiente entity = inventarioMapper.toPendienteEntity(dto);
        InventarioPendiente guardado = inventarioPendienteRepository.save(entity);

        InventarioPendienteDTO responseDto = inventarioMapper.toPendienteDTO(guardado);
        responseDto.setProveedorId(dto.getProveedorId());
        responseDto.setDescripcionLote(dto.getDescripcionLote());
        responseDto.setPrecioLote(dto.getPrecioLote());
        responseDto.setFechaVencimiento(dto.getFechaVencimiento());
        return responseDto;
    }

    @Transactional
    public InventarioPendienteDTO actualizarPendiente(Integer id, RegistroPendienteRequestDTO dto) {
        // 1. Buscar la entidad por su ID (validando que no tenga marca de baja)
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro pendiente no encontrado con id: " + id));

        // Validar que se encuentre en espera (estado 1) u observado para corrección (estado 4)
        if (pendiente.getEstado() != null && pendiente.getEstado() != 1 && pendiente.getEstado() != 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden modificar registros pendientes en estado de espera (1) u observado (4)");
        }

        // 2. Actualizar únicamente las columnas que existen físicamente en inventario_pendientes
        if (dto.getNombreProducto() != null && !dto.getNombreProducto().trim().isEmpty()) {
            pendiente.setNombreProducto(dto.getNombreProducto().trim());
        }
        if (dto.getCodigoBarras() != null) {
            pendiente.setCodigoBarras(dto.getCodigoBarras().trim());
        }
        if (dto.getCategoriaId() != null) {
            pendiente.setCategoriaId(dto.getCategoriaId());
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
        if (dto.getProductoId() != null) {
            pendiente.setProductoId(dto.getProductoId());
        }

        // 3. Flujo 4 -> 1: Si estaba en estado 4 (observado) y el cajero lo rectifica, vuelve a estado 1 (Pendiente de Revisión)
        pendiente.setEstado(1);
        pendiente.setFechaRevision(null);
        pendiente.setUsuarioRevision(null);

        // 4. Guardar los cambios usando el método .save() estándar de JPA
        InventarioPendiente actualizado = inventarioPendienteRepository.save(pendiente);

        // 5. Devolver DTO preservando los datos del lote en tránsito
        InventarioPendienteDTO responseDto = inventarioMapper.toPendienteDTO(actualizado);
        responseDto.setProveedorId(dto.getProveedorId());
        responseDto.setDescripcionLote(dto.getDescripcionLote());
        responseDto.setPrecioLote(dto.getPrecioLote());
        responseDto.setFechaVencimiento(dto.getFechaVencimiento());
        responseDto.setObservacionRevision(observacionesPendientes.get(id));
        return responseDto;
    }

    @Transactional
    public void aprobarPendiente(Integer id, RegistroPendienteRequestDTO cambios, Integer usuarioRevisionParam) {
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Registro pendiente no encontrado con id: " + id));

        if (pendiente.getEstado() != null && pendiente.getEstado() != 1 && pendiente.getEstado() != 4) {
            throw new IllegalStateException("Solo se pueden aprobar registros en estado de espera (1) u observado (4)");
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

        // Resolucion de parametros del lote en transito (DTO / Consultas JPA con JOIN)
        Integer proveedorId = (cambios != null) ? cambios.getProveedorId() : null;
        String descripcionLote = (cambios != null) ? cambios.getDescripcionLote() : null;
        BigDecimal precioLote = (cambios != null) ? cambios.getPrecioLote() : null;
        LocalDate fechaVencimiento = (cambios != null) ? cambios.getFechaVencimiento() : null;

        // Si es producto existente y no se envio proveedor, resolver el proveedor habitual mediante @Query con JOIN
        if (proveedorId == null && pendiente.getProductoId() != null) {
            List<Lote> ultimosLotes = loteRepository.findUltimosLotesPorProducto(pendiente.getProductoId());
            if (!ultimosLotes.isEmpty() && ultimosLotes.get(0).getProveedorId() != null) {
                proveedorId = ultimosLotes.get(0).getProveedorId();
            }
        }

        if (descripcionLote == null || descripcionLote.trim().isEmpty()) {
            descripcionLote = "Lote " + (pendiente.getCodigoInterno() != null ? pendiente.getCodigoInterno() : "Ingreso");
        }

        if (precioLote == null && pendiente.getPrecioCompra() != null && pendiente.getStockIngresado() != null) {
            precioLote = pendiente.getPrecioCompra().multiply(BigDecimal.valueOf(pendiente.getStockIngresado()));
        }

        // Inyeccion directa como parametros al procedimiento almacenado
        inventarioProcedureRepository.ejecutarAprobarPendiente(
                id, usuarioRevision, proveedorId, descripcionLote, precioLote, fechaVencimiento);

        observacionesPendientes.remove(id);
    }

    @Transactional
    public void rechazarPendiente(Integer id, String motivo, Integer usuarioRevisionParam) {
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Registro pendiente no encontrado con id: " + id));

        if (pendiente.getEstado() != null && pendiente.getEstado() != 1 && pendiente.getEstado() != 4) {
            throw new IllegalStateException("Solo se pueden rechazar registros en estado de espera (1) u observado (4)");
        }

        Integer usuarioRevision = usuarioRevisionParam != null ? usuarioRevisionParam : 1;
        pendiente.setEstado(3); // 3 = Rechazado / Anulado
        pendiente.setFechaRevision(LocalDateTime.now());
        pendiente.setUsuarioRevision(usuarioRevision);

        inventarioPendienteRepository.save(pendiente);

        if (motivo != null && !motivo.trim().isEmpty()) {
            observacionesPendientes.put(id, motivo.trim());
        }
    }

    @Transactional
    public void rechazarPendiente(Integer id, Integer usuarioRevisionParam) {
        rechazarPendiente(id, null, usuarioRevisionParam);
    }

    @Transactional
    public void observarPendiente(Integer id, String observacion, Integer usuarioRevisionParam) {
        InventarioPendiente pendiente = inventarioPendienteRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Registro pendiente no encontrado con id: " + id));

        if (pendiente.getEstado() != null && pendiente.getEstado() != 1 && pendiente.getEstado() != 4) {
            throw new IllegalStateException("Solo se pueden mandar a corregir registros en estado de espera (1) u observado (4)");
        }

        Integer usuarioRevision = usuarioRevisionParam != null ? usuarioRevisionParam : 1;
        pendiente.setEstado(4); // 4 = Observado / Para Corrección
        pendiente.setFechaRevision(LocalDateTime.now());
        pendiente.setUsuarioRevision(usuarioRevision);

        inventarioPendienteRepository.save(pendiente);

        if (observacion != null && !observacion.trim().isEmpty()) {
            observacionesPendientes.put(id, observacion.trim());
        }
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
        inventario.setStock(0);
        inventarioRepository.save(inventario);
    }

    public List<ProveedorDTO> listarProveedoresActivos() {
        return proveedorRepository.findByMarcaBajaFalseOrderByNombreEmpresaAsc().stream()
                .map(inventarioMapper::toProveedorDTO)
                .collect(Collectors.toList());
    }

    public List<CategoriaDTO> listarCategoriasActivas() {
        return conceptoRepository.findByGrupoIdAndMarcaBajaFalseOrderByCodigoAsc(2).stream()
                .filter(c -> c.getCodigo() != null && c.getCodigo() > 0)
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
        if (dto.getProductoId() != null) {
            pendiente.setProductoId(dto.getProductoId());
        }
    }
}
