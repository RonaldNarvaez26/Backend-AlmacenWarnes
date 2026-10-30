package com.almacen.warnes.modulo_inventario.service;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorCatalogoProductoProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorHistorialLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProveedorListadoProjection;
import com.almacen.warnes.modulo_inventario.entity.Proveedor;
import com.almacen.warnes.modulo_inventario.repository.ProveedorProcedureRepository;
import com.almacen.warnes.modulo_inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorProcedureRepository proveedorProcedureRepository;

    /**
     * Obtiene indicadores clave (KPIs) del módulo de proveedores
     */
    public ProveedorKpiDTO obtenerKpiResumen() {
        ProveedorKpiProjection p = proveedorRepository.obtenerKpiResumen();
        if (p == null) {
            return ProveedorKpiDTO.builder()
                    .totalProveedoresActivos(0L)
                    .proveedoresAlertasVencimiento(0L)
                    .totalProductosAbastecidos(0L)
                    .totalLotesActivos(0L)
                    .valorTotalInventario(BigDecimal.ZERO)
                    .build();
        }

        return ProveedorKpiDTO.builder()
                .totalProveedoresActivos(p.getTotal_proveedores_activos() != null ? p.getTotal_proveedores_activos() : 0L)
                .proveedoresAlertasVencimiento(p.getProveedores_alertas_vencimiento() != null ? p.getProveedores_alertas_vencimiento() : 0L)
                .totalProductosAbastecidos(p.getTotal_productos_abastecidos() != null ? p.getTotal_productos_abastecidos() : 0L)
                .totalLotesActivos(p.getTotal_lotes_activos() != null ? p.getTotal_lotes_activos() : 0L)
                .valorTotalInventario(p.getValor_total_inventario() != null ? p.getValor_total_inventario() : BigDecimal.ZERO)
                .build();
    }

    /**
     * Lista distribuidores con filtros cruzados de empresa/contacto/contrato o producto suministrado
     */
    public List<ProveedorListadoDTO> listarProveedores(String busqueda, String producto) {
        List<ProveedorListadoProjection> projections = proveedorRepository.listarProveedores(busqueda, producto);
        return projections.stream().map(p -> ProveedorListadoDTO.builder()
                .proveedorId(p.getProveedor_id())
                .nombreEmpresa(p.getNombre_empresa())
                .nombreContacto(p.getNombre_contacto())
                .numeroContacto(p.getNumero_contacto())
                .anotaciones(p.getAnotaciones())
                .marcaBaja(p.getMarca_baja())
                .fechaCreacion(p.getFecha_creacion())
                .totalProductos(p.getTotal_productos() != null ? p.getTotal_productos() : 0L)
                .totalLotes(p.getTotal_lotes() != null ? p.getTotal_lotes() : 0L)
                .lotesActivos(p.getLotes_activos() != null ? p.getLotes_activos() : 0L)
                .lotesEnRiesgo(p.getLotes_en_riesgo() != null ? p.getLotes_en_riesgo() : 0L)
                .stockTotalAcumulado(p.getStock_total_acumulado() != null ? p.getStock_total_acumulado() : 0L)
                .valorMercaderia(p.getValor_mercaderia() != null ? p.getValor_mercaderia() : BigDecimal.ZERO)
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Obtiene los datos simples de un proveedor por ID
     */
    public ProveedorDTO obtenerPorId(Integer proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .filter(p -> !Boolean.TRUE.equals(p.getMarcaBaja()))
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + proveedorId));

        return ProveedorDTO.builder()
                .proveedorId(proveedor.getProveedorId())
                .nombreEmpresa(proveedor.getNombreEmpresa())
                .nombreContacto(proveedor.getNombreContacto())
                .numeroContacto(proveedor.getNumeroContacto())
                .anotaciones(proveedor.getAnotaciones())
                .build();
    }

    /**
     * Registra un nuevo distribuidor ejecutando spProveedorRegistrar
     */
    @Transactional
    public Integer registrarProveedor(CrearProveedorRequestDTO dto) {
        log.info("Registrando distribuidor: {}", dto.getNombreEmpresa());
        Integer usuario = dto.getUsuarioCreacion() != null ? dto.getUsuarioCreacion() : 1;
        return proveedorRepository.registrarProveedor(
                dto.getNombreEmpresa(),
                dto.getNombreContacto(),
                dto.getNumeroContacto(),
                dto.getAnotaciones(),
                usuario
        );
    }

    /**
     * Actualiza datos y contrato vigente del distribuidor mediante spProveedorActualizar
     */
    @Transactional
    public void actualizarProveedor(Integer id, ActualizarProveedorRequestDTO dto) {
        log.info("Actualizando distribuidor ID: {}", id);
        Integer usuario = dto.getUsuarioModificacion() != null ? dto.getUsuarioModificacion() : 1;
        proveedorProcedureRepository.ejecutarActualizarProveedor(
                id,
                dto.getNombreEmpresa(),
                dto.getNombreContacto(),
                dto.getNumeroContacto(),
                dto.getAnotaciones(),
                usuario
        );
    }

    /**
     * Da de baja lógica a un distribuidor mediante spProveedorEliminar
     */
    @Transactional
    public void eliminarProveedor(Integer id) {
        log.info("Dando de baja logica al distribuidor ID: {}", id);
        proveedorProcedureRepository.ejecutarEliminarProveedor(id, 1);
    }

    /**
     * Catálogo de productos abastecidos por un proveedor específico
     */
    public List<ProveedorCatalogoProductoDTO> listarCatalogoProductos(Integer proveedorId) {
        List<ProveedorCatalogoProductoProjection> projections = proveedorRepository.listarCatalogoProductos(proveedorId);
        return projections.stream().map(p -> ProveedorCatalogoProductoDTO.builder()
                .productoId(p.getProducto_id())
                .codigoInterno(p.getCodigo_interno())
                .codigoBarras(p.getCodigo_barras())
                .nombreProducto(p.getNombre_producto())
                .categoriaNombre(p.getCategoria_nombre())
                .precioCompra(p.getPrecio_compra())
                .precioVenta(p.getPrecio_venta())
                .estadoProducto(p.getEstado_producto())
                .stockActual(p.getStock_actual() != null ? p.getStock_actual() : 0L)
                .totalLotesRecibidos(p.getTotal_lotes_recibidos() != null ? p.getTotal_lotes_recibidos() : 0L)
                .fechaUltimaEntrega(p.getFecha_ultima_entrega())
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Historial de lotes entregados y estado de vencimiento
     */
    public List<ProveedorHistorialLoteDTO> listarHistorialLotes(Integer proveedorId, String filtro) {
        String filtroEstado = (filtro != null && !filtro.trim().isEmpty()) ? filtro.trim() : "todos";
        List<ProveedorHistorialLoteProjection> projections = proveedorRepository.listarHistorialLotes(proveedorId, filtroEstado);
        return projections.stream().map(p -> ProveedorHistorialLoteDTO.builder()
                .loteId(p.getLote_id())
                .descripcion(p.getDescripcion())
                .productoId(p.getProducto_id())
                .nombreProducto(p.getNombre_producto())
                .codigoInterno(p.getCodigo_interno())
                .cantidadInicial(p.getCantidad_inicial())
                .stockActual(p.getStock_actual())
                .costoUnitario(p.getCosto_unitario())
                .precioLote(p.getPrecio_lote())
                .fechaVencimiento(p.getFecha_vencimiento())
                .diasParaVencer(p.getDias_para_vencer())
                .estadoCaducidad(p.getEstado_caducidad())
                .fechaIngreso(p.getFecha_ingreso())
                .build()
        ).collect(Collectors.toList());
    }
}
