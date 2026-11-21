package com.almacen.warnes.modulo_ventas.model;

import lombok.*;
import java.math.BigDecimal;

/**
 * Entidad que representa la tabla Detalles_Ventas.
 * Registra cada línea del carrito: qué producto, de qué lote (inventario_id),
 * a qué precio fue congelado y la cantidad.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    private Integer detalleVentaId;

    /** FK → Productos */
    private Integer productoId;

    /** FK → Ventas */
    private Integer ventaId;

    /**
     * FK → Inventario — identifica exactamente de qué lote/sucursal se descontó el stock.
     * Es la clave para la lógica PEPS (primero en entrar, primero en salir).
     */
    private Integer inventarioId;

    /**
     * Precio congelado al momento de la venta (no cambia aunque cambie Productos.precio_venta).
     * Puede ser Productos.precio_venta o Inventario.precio_descuento si estaba en oferta.
     */
    private BigDecimal precioUnitario;

    /** decimal(8,2) según esquema de Detalles_Ventas */
    private BigDecimal cantidad;

    private Boolean marcaBaja;
}

