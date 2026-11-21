package com.almacen.warnes.modulo_ventas.model;

import lombok.*;
import java.math.BigDecimal;

/**
 * Proyección de la función fn_pos_buscar_producto(p_sucursal_id, p_criterio).
 * No corresponde a una tabla directa; es el resultado del buscador ágil POS
 * que combina Inventario + Productos con prioridad PEPS (stock DESC, fecha_creacion ASC).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoPOS {

    /** ID del registro en Inventario — necesario para sp_ventas_registrar */
    private Integer inventarioId;

    private Integer productoId;
    private String codigoBarras;
    private String codigoInterno;
    private String nombreProducto;

    /** Precio oficial vigente de Productos.precio_venta */
    private BigDecimal precioVenta;

    /** Unidades disponibles en este lote para esta sucursal */
    private Integer stockDisponible;

    /** true si Inventario.en_descuento = true para este lote */
    private Boolean enDescuento;

    /**
     * Precio especial del lote cuando en_descuento=true.
     * Nullable — el cajero puede elegir precio normal o precio descuento.
     */
    private BigDecimal precioDescuento;
}

