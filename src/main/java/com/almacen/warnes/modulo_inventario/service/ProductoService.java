package com.almacen.warnes.modulo_inventario.service;

import com.almacen.warnes.modulo_inventario.dto.ProductoDTO;
import com.almacen.warnes.modulo_inventario.entity.Producto;
import com.almacen.warnes.modulo_inventario.mapper.ProductoMapper;
import com.almacen.warnes.modulo_inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public List<ProductoDTO> listarProductosActivos() {
        return productoRepository.findByMarcaBajaFalse().stream()
                .map(productoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ProductoDTO obtenerPorId(Integer id) {
        return productoRepository.findById(id)
                .map(productoMapper::toDTO)
                .orElse(null);
    }

    public ProductoDTO obtenerPorCodigoInterno(String codigoInterno) {
        return productoRepository.findByCodigoInternoAndMarcaBajaFalse(codigoInterno)
                .map(productoMapper::toDTO)
                .orElse(null);
    }

    public ProductoDTO obtenerPorCodigoBarras(String codigoBarras) {
        return productoRepository.findByCodigoBarrasAndMarcaBajaFalse(codigoBarras)
                .map(productoMapper::toDTO)
                .orElse(null);
    }
}
