package com.almacen.warnes.modulo_inventario.mapper;

import com.almacen.warnes.modulo_inventario.dto.ProductoDTO;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoDTO toDTO(Producto entity) {
        if (entity == null) {
            return null;
        }
        return ProductoDTO.builder()
                .productoId(entity.getProductoId())
                .categoriaId(entity.getCategoriaId())
                .codigoBarras(entity.getCodigoBarras())
                .nombreProducto(entity.getNombreProducto())
                .codigoInterno(entity.getCodigoInterno())
                .precioCompra(entity.getPrecioCompra())
                .precioVenta(entity.getPrecioVenta())
                .estado(entity.getEstado())
                .marcaBaja(entity.getMarcaBaja())
                .fechaCreacion(entity.getFechaCreacion())
                .usuarioCreacion(entity.getUsuarioCreacion())
                .fechaModificacion(entity.getFechaModificacion())
                .usuarioModificacion(entity.getUsuarioModificacion())
                .build();
    }

    public Producto toEntity(ProductoDTO dto) {
        if (dto == null) {
            return null;
        }
        return Producto.builder()
                .productoId(dto.getProductoId())
                .categoriaId(dto.getCategoriaId())
                .codigoBarras(dto.getCodigoBarras())
                .nombreProducto(dto.getNombreProducto())
                .codigoInterno(dto.getCodigoInterno())
                .precioCompra(dto.getPrecioCompra())
                .precioVenta(dto.getPrecioVenta())
                .estado(dto.getEstado())
                .marcaBaja(dto.getMarcaBaja() != null ? dto.getMarcaBaja() : false)
                .fechaCreacion(dto.getFechaCreacion())
                .usuarioCreacion(dto.getUsuarioCreacion())
                .fechaModificacion(dto.getFechaModificacion())
                .usuarioModificacion(dto.getUsuarioModificacion())
                .build();
    }
}
