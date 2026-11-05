package com.almacen.warnes.modulo_inventario.mapper;

import com.almacen.warnes.modulo_inventario.dto.*;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioKpiProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.InventarioLoteProjection;
import com.almacen.warnes.modulo_inventario.dto.projection.ProductoCatalogoProjection;
import com.almacen.warnes.modulo_inventario.entity.Concepto;
import com.almacen.warnes.modulo_inventario.entity.InventarioPendiente;
import com.almacen.warnes.modulo_inventario.entity.Proveedor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InventarioMapper {

    public InventarioKpiDTO toKpiDTO(InventarioKpiProjection projection) {
        if (projection == null) {
            return null;
        }
        return InventarioKpiDTO.builder()
                .totalProductosCatalogo(projection.getTotal_productos_catalogo())
                .unidadesStockActivo(projection.getUnidades_stock_activo())
                .lotesSalaEspera(projection.getLotes_sala_espera())
                .alertasVencimiento(projection.getAlertas_vencimiento())
                .build();
    }

    public InventarioLoteDTO toLoteDTO(InventarioLoteProjection projection) {
        if (projection == null) {
            return null;
        }
        return InventarioLoteDTO.builder()
                .inventarioId(projection.getInventario_id())
                .stock(projection.getStock())
                .enDescuento(projection.getEn_descuento())
                .precioDescuento(projection.getPrecio_descuento())
                .productoId(projection.getProducto_id())
                .codigoInterno(projection.getCodigo_interno())
                .codigoBarras(projection.getCodigo_barras())
                .nombreProducto(projection.getNombre_producto())
                .precioVenta(projection.getPrecio_venta())
                .estadoProducto(projection.getEstado_producto())
                .loteId(projection.getLote_id())
                .descripcionLote(projection.getDescripcion_lote())
                .costoUnitario(projection.getCosto_unitario())
                .fechaVencimiento(projection.getFecha_vencimiento())
                .nombreProveedor(projection.getNombre_proveedor())
                .diasParaVencer(projection.getDias_para_vencer())
                .build();
    }

    public ProductoCatalogoDTO toCatalogoDTO(ProductoCatalogoProjection projection) {
        if (projection == null) {
            return null;
        }
        return ProductoCatalogoDTO.builder()
                .productoId(projection.getProducto_id())
                .codigoInterno(projection.getCodigo_interno())
                .codigoBarras(projection.getCodigo_barras())
                .nombreProducto(projection.getNombre_producto())
                .categoriaId(projection.getCategoria_id())
                .precioCompra(projection.getPrecio_compra())
                .precioVenta(projection.getPrecio_venta())
                .estado(projection.getEstado())
                .stockTotalAcumulado(projection.getStock_total_acumulado())
                .build();
    }

    public InventarioPendienteDTO toPendienteDTO(InventarioPendiente entity) {
        if (entity == null) {
            return null;
        }
        return InventarioPendienteDTO.builder()
                .inventarioPendienteId(entity.getInventarioPendienteId())
                .sucursalId(entity.getSucursalId())
                .productoId(entity.getProductoId())
                .nombreProducto(entity.getNombreProducto())
                .codigoBarras(entity.getCodigoBarras())
                .codigoInterno(entity.getCodigoInterno())
                .categoriaId(entity.getCategoriaId())
                .precioCompra(entity.getPrecioCompra())
                .precioVenta(entity.getPrecioVenta())
                .stockIngresado(entity.getStockIngresado())
                .estado(entity.getEstado())
                .proveedorId(entity.getProveedorId())
                .descripcionLote(entity.getDescripcionLote())
                .precioLote(entity.getPrecioLote())
                .fechaVencimiento(entity.getFechaVencimiento())
                .marcaBaja(entity.getMarcaBaja())
                .fechaCreacion(entity.getFechaCreacion())
                .usuarioCreacion(entity.getUsuarioCreacion())
                .fechaRevision(entity.getFechaRevision())
                .usuarioRevision(entity.getUsuarioRevision())
                .build();
    }

    public InventarioPendiente toPendienteEntity(RegistroPendienteRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return InventarioPendiente.builder()
                .sucursalId(dto.getSucursalId() != null ? dto.getSucursalId() : 1)
                .productoId(dto.getProductoId())
                .nombreProducto(dto.getNombreProducto())
                .codigoBarras(dto.getCodigoBarras() != null ? dto.getCodigoBarras().trim() : null)
                .codigoInterno(dto.getCodigoInterno() != null ? dto.getCodigoInterno().trim() : null)
                .categoriaId(dto.getCategoriaId())
                .precioCompra(dto.getPrecioCompra())
                .precioVenta(dto.getPrecioVenta())
                .stockIngresado(dto.getStockIngresado())
                .estado(1) // Sala de espera
                .proveedorId(dto.getProveedorId())
                .descripcionLote(dto.getDescripcionLote())
                .precioLote(dto.getPrecioLote())
                .fechaVencimiento(dto.getFechaVencimiento())
                .marcaBaja(false)
                .fechaCreacion(LocalDateTime.now())
                .usuarioCreacion(dto.getUsuarioCreacion() != null ? dto.getUsuarioCreacion() : 1)
                .build();
    }

    public ProveedorDTO toProveedorDTO(Proveedor entity) {
        if (entity == null) {
            return null;
        }
        return ProveedorDTO.builder()
                .proveedorId(entity.getProveedorId())
                .nombreEmpresa(entity.getNombreEmpresa())
                .nombreContacto(entity.getNombreContacto())
                .numeroContacto(entity.getNumeroContacto())
                .anotaciones(entity.getAnotaciones())
                .marcaBaja(entity.getMarcaBaja())
                .build();
    }

    public CategoriaDTO toCategoriaDTO(Concepto entity) {
        if (entity == null) {
            return null;
        }
        return CategoriaDTO.builder()
                .categoriaId(entity.getCodigo())
                .codigo(entity.getCodigo())
                .nombreCategoria(entity.getDescripcion())
                .descripcion(entity.getDescripcion())
                .build();
    }
}
