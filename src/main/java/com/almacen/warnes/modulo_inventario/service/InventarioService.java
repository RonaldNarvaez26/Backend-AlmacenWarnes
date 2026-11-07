package com.almacen.warnes.modulo_inventario.service;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioPendienteDetalleProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProductoCatalogoProjection;
import com.almacen.warnes.modulo_inventario.entity.Concepto;
import com.almacen.warnes.modulo_inventario.entity.Inventario;
import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import com.almacen.warnes.modulo_inventario.entity.Lote;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import com.almacen.warnes.modulo_inventario.entity.Proveedor;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

    public InventarioKpiDTO obtenerKpiResumen(Integer sucursalId) {
        InventarioKpiProjection projection = inventarioRepository.obtenerKpiResumen(sucursalId);
        return inventarioMapper.toKpiDTO(projection);
    }

    public InventarioKpiDTO obtenerKpiResumen() {
        return obtenerKpiResumen(null);
    }

    public InventarioKpiProjection obtenerKpiResumenProyeccion(Integer sucursalId) {
        return inventarioRepository.obtenerKpiResumen(sucursalId);
    }

    public InventarioKpiProjection obtenerKpiResumenProyeccion() {
        return obtenerKpiResumenProyeccion(null);
    }

    public List<InventarioLoteDTO> listarLotes(String estado, Integer sucursalId) {
        String estadoFiltro = (estado != null && !estado.trim().isEmpty()) ? estado.trim().toUpperCase() : "ACTIVOS";
        return inventarioRepository.listarLotes(estadoFiltro, sucursalId).stream()
                .map(inventarioMapper::toLoteDTO)
                .collect(Collectors.toList());
    }

    public List<InventarioLoteDTO> listarLotes(String estado) {
        return listarLotes(estado, null);
    }

    public List<InventarioLoteDTO> listarLotes() {
        return listarLotes("ACTIVOS", null);
    }

    public List<InventarioLoteProjection> listarLotesProyeccion() {
        return inventarioRepository.listarLotes("ACTIVOS", null);
    }

    public List<ProductoCatalogoDTO> listarCatalogo(String estado, Integer sucursalId) {
        String estadoFiltro = (estado != null && !estado.trim().isEmpty()) ? estado.trim().toUpperCase() : "ACTIVOS";
        return productoRepository.listarCatalogo(estadoFiltro, sucursalId).stream()
                .map(inventarioMapper::toCatalogoDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoCatalogoDTO> listarCatalogo(String estado) {
        return listarCatalogo(estado, null);
    }

    public List<ProductoCatalogoDTO> listarCatalogo() {
        return listarCatalogo("ACTIVOS", null);
    }

    public List<ProductoCatalogoProjection> listarCatalogoProyeccion() {
        return productoRepository.listarCatalogo("ACTIVOS", null);
    }

    public List<SucursalDTO> listarSucursales() {
        return jdbcTemplate.query(
                "SELECT sucursal_id, codigo_interno, ciudad, direccion, telefono, marca_baja FROM sucursales WHERE marca_baja = false ORDER BY sucursal_id ASC",
                (rs, rowNum) -> SucursalDTO.builder()
                        .sucursalId(rs.getInt("sucursal_id"))
                        .codigoInterno(rs.getString("codigo_interno"))
                        .ciudad(rs.getString("ciudad"))
                        .direccion(rs.getString("direccion"))
                        .telefono(rs.getString("telefono"))
                        .marcaBaja(rs.getBoolean("marca_baja"))
                        .build()
        );
    }

    // --- Sala de Espera (Paso 2) ---

    /**
     * Lista registros en espera combinando la tabla inventario_pendientes con
     * el último lote conocido del producto mediante @Query con JOIN, sin tocar el esquema de BD.
     */
    public List<InventarioPendienteDTO> listarPendientesActivos(Integer sucursalId) {
        List<InventarioPendienteDetalleProjection> projections = inventarioPendienteRepository.findAllConDetalleLote(sucursalId);
        if (projections != null && !projections.isEmpty()) {
            return projections.stream().map(p -> {
                BigDecimal precioLote = (p.getPrecioCompra() != null && p.getStockIngresado() != null)
                        ? p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockIngresado()))
                        : null;
                return InventarioPendienteDTO.builder()
                        .inventarioPendienteId(p.getInventarioPendienteId())
                        .sucursalId(p.getSucursalId())
                        .codigoSucursal(p.getCodigoSucursal())
                        .ciudadSucursal(p.getCiudadSucursal())
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
                        .nombreProveedor(p.getNombreProveedor())
                        .descripcionLote(p.getDescripcionLote() != null ? p.getDescripcionLote() : "Lote " + p.getCodigoInterno())
                        .precioLote(precioLote)
                        .fechaVencimiento(p.getFechaVencimiento())
                        .observacionRevision(p.getObservacionRevision() != null ? p.getObservacionRevision() : observacionesPendientes.get(p.getInventarioPendienteId()))
                        .fechaSubsanacion(p.getFechaSubsanacion())
                        .nombreUsuarioCreacion(p.getNombreUsuarioCreacion() != null ? p.getNombreUsuarioCreacion() : p.getNombre_usuario_creacion())
                        .rolUsuarioCreacion(p.getRolUsuarioCreacion() != null ? p.getRolUsuarioCreacion() : p.getRol_usuario_creacion())
                        .nombreUsuarioRevision(p.getNombreUsuarioRevision() != null ? p.getNombreUsuarioRevision() : p.getNombre_usuario_revision())
                        .build();
            }).collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }

    public List<InventarioPendienteDTO> listarPendientesActivos() {
        return listarPendientesActivos(null);
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

        // 2. Actualizar las columnas de inventario_pendientes
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
        if (dto.getProveedorId() != null) {
            pendiente.setProveedorId(dto.getProveedorId());
        }
        if (dto.getDescripcionLote() != null && !dto.getDescripcionLote().trim().isEmpty()) {
            pendiente.setDescripcionLote(dto.getDescripcionLote().trim());
        }
        if (dto.getFechaVencimiento() != null) {
            pendiente.setFechaVencimiento(dto.getFechaVencimiento());
        }
        if (dto.getObservacionRevision() != null) {
            pendiente.setObservacionRevision(dto.getObservacionRevision().trim());
            observacionesPendientes.put(id, dto.getObservacionRevision().trim());
        }
        if (dto.getFechaSubsanacion() != null) {
            pendiente.setFechaSubsanacion(dto.getFechaSubsanacion());
        } else {
            pendiente.setFechaSubsanacion(LocalDateTime.now());
        }

        // 3. Flujo 4 -> 1: Si estaba en estado 4 (observado) y el cajero lo rectifica, vuelve a estado 1 (Pendiente de Revisión)
        pendiente.setEstado(1);
        pendiente.setFechaRevision(null);
        pendiente.setUsuarioRevision(null);

        // 4. Guardar los cambios usando el método .save() estándar de JPA
        InventarioPendiente actualizado = inventarioPendienteRepository.save(pendiente);

        // 5. Devolver DTO persistido
        InventarioPendienteDTO responseDto = inventarioMapper.toPendienteDTO(actualizado);
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

        // Resolucion de parametros del lote (tomando de cambios si vienen, o de pendiente)
        Integer proveedorId = (cambios != null && cambios.getProveedorId() != null)
                ? cambios.getProveedorId()
                : pendiente.getProveedorId();
        String descripcionLote = (cambios != null && cambios.getDescripcionLote() != null && !cambios.getDescripcionLote().trim().isEmpty())
                ? cambios.getDescripcionLote()
                : pendiente.getDescripcionLote();
        BigDecimal precioLote = (cambios != null && cambios.getPrecioLote() != null)
                ? cambios.getPrecioLote()
                : pendiente.getPrecioLote();
        LocalDate fechaVencimiento = (cambios != null && cambios.getFechaVencimiento() != null)
                ? cambios.getFechaVencimiento()
                : pendiente.getFechaVencimiento();

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

        if (motivo != null && !motivo.trim().isEmpty()) {
            pendiente.setObservacionRevision(motivo.trim());
            observacionesPendientes.put(id, motivo.trim());
        }

        inventarioPendienteRepository.save(pendiente);
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

        if (observacion != null && !observacion.trim().isEmpty()) {
            pendiente.setObservacionRevision(observacion.trim());
            observacionesPendientes.put(id, observacion.trim());
        }

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
        inventario.setStock(0);
        inventarioRepository.save(inventario);
    }

    @Transactional
    public void reactivarProducto(Integer productoId, Integer usuarioModificacion) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con id: " + productoId));
        producto.setMarcaBaja(false);
        producto.setFechaModificacion(LocalDateTime.now());
        producto.setUsuarioModificacion(usuarioModificacion != null ? usuarioModificacion : 1);
        productoRepository.save(producto);
    }

    @Transactional
    public void reactivarLoteInventario(Integer inventarioId, Integer usuarioModificacion) {
        Inventario inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de inventario no encontrado con id: " + inventarioId));

        inventario.setMarcaBaja(false);
        inventario.setFechaModificacion(LocalDateTime.now());
        inventario.setUsuarioModificacion(usuarioModificacion != null ? usuarioModificacion : 1);

        Lote lote = loteRepository.findById(inventario.getLoteId()).orElse(null);
        if (lote != null) {
            lote.setMarcaBaja(false);
            loteRepository.save(lote);
            if (inventario.getStock() == null || inventario.getStock() <= 0) {
                inventario.setStock(lote.getCantidad() != null && lote.getCantidad() > 0 ? lote.getCantidad() : 1);
            }
        } else if (inventario.getStock() == null || inventario.getStock() <= 0) {
            inventario.setStock(1);
        }

        inventarioRepository.save(inventario);
    }

    @Transactional
    public void descontarUnidades(Integer inventarioId, DescontarUnidadesRequestDTO dto) {
        if (inventarioId == null || inventarioId <= 0) {
            throw new IllegalArgumentException("El identificador de inventario debe ser válido");
        }
        if (dto == null || dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero");
        }
        inventarioProcedureRepository.ejecutarDescontarUnidades(
                inventarioId,
                dto.getCantidad(),
                dto.getUsuarioId(),
                dto.getMotivo()
        );
    }

    public List<ProveedorDTO> listarProveedoresActivos() {
        return listarProveedoresActivos(null);
    }

    public List<ProveedorDTO> listarProveedoresActivos(Integer sucursalId) {
        List<Proveedor> lista = (sucursalId != null)
                ? proveedorRepository.findBySucursalIdAndMarcaBajaFalseOrderByNombreEmpresaAsc(sucursalId)
                : proveedorRepository.findByMarcaBajaFalseOrderByNombreEmpresaAsc();
        return lista.stream()
                .map(inventarioMapper::toProveedorDTO)
                .collect(Collectors.toList());
    }

    public List<CategoriaDTO> listarCategoriasActivas() {
        return conceptoRepository.findByGrupoIdAndMarcaBajaFalseOrderByCodigoAsc(2).stream()
                .filter(c -> c.getCodigo() != null && c.getCodigo() > 0)
                .map(inventarioMapper::toCategoriaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CategoriaDTO> crearCategoria(CrearCategoriaRequestDTO request) {
        if (request == null || request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        String nombre = request.getNombre().trim();
        if (nombre.length() > 80) {
            throw new IllegalArgumentException("El nombre de la categoría no puede superar los 80 caracteres");
        }

        boolean existe = conceptoRepository.existsByGrupoIdAndDescripcionIgnoreCaseAndMarcaBajaFalse(2, nombre);
        if (existe) {
            throw new IllegalArgumentException("Ya existe una categoría activa con el nombre '" + nombre + "'");
        }

        Integer siguienteCodigo = conceptoRepository.obtenerSiguienteCodigo(2);
        Integer siguienteConceptoId = conceptoRepository.obtenerSiguienteConceptoIdSequence();

        Concepto nuevoConcepto = Concepto.builder()
                .conceptoId(siguienteConceptoId)
                .grupoId(2)
                .codigo(siguienteCodigo)
                .descripcion(nombre)
                .marcaBaja(false)
                .fechaCreacion(LocalDateTime.now())
                .usuarioCreacion(request.getUsuarioCreacion() != null ? request.getUsuarioCreacion() : 1)
                .build();

        conceptoRepository.saveAndFlush(nuevoConcepto);

        return listarCategoriasActivas();
    }

    public List<CategoriaDTO> listarTodasCategorias() {
        return conceptoRepository.findByGrupoIdAndCodigoGreaterThanOrderByCodigoAsc(2, 0).stream()
                .map(inventarioMapper::toCategoriaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaDTO actualizarCategoria(Integer codigo, String nuevoNombre) {
        if (codigo == null || codigo <= 0) {
            throw new IllegalArgumentException("Código de categoría no válido");
        }
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede quedar vacío");
        }
        Concepto concepto = conceptoRepository.findByGrupoIdAndCodigo(2, codigo)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la categoría con código: " + codigo));

        concepto.setDescripcion(nuevoNombre.trim());
        conceptoRepository.save(concepto);

        return inventarioMapper.toCategoriaDTO(concepto);
    }

    @Transactional
    public CategoriaDTO cambiarEstadoCategoria(Integer codigo, Boolean marcaBaja) {
        if (codigo == null || codigo <= 0) {
            throw new IllegalArgumentException("Código de categoría no válido");
        }
        Concepto concepto = conceptoRepository.findByGrupoIdAndCodigo(2, codigo)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la categoría con código: " + codigo));

        concepto.setMarcaBaja(Boolean.TRUE.equals(marcaBaja));
        conceptoRepository.save(concepto);

        return inventarioMapper.toCategoriaDTO(concepto);
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
        if (dto.getProveedorId() != null) {
            pendiente.setProveedorId(dto.getProveedorId());
        }
        if (dto.getDescripcionLote() != null && !dto.getDescripcionLote().trim().isEmpty()) {
            pendiente.setDescripcionLote(dto.getDescripcionLote().trim());
        }
        if (dto.getFechaVencimiento() != null) {
            pendiente.setFechaVencimiento(dto.getFechaVencimiento());
        }
        if (dto.getObservacionRevision() != null) {
            pendiente.setObservacionRevision(dto.getObservacionRevision().trim());
        }
        if (dto.getFechaSubsanacion() != null) {
            pendiente.setFechaSubsanacion(dto.getFechaSubsanacion());
        }
    }
}
