package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "producto_id")
    private Integer productoId;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;

    @Column(name = "codigo_barras", nullable = false, length = 100)
    @Builder.Default
    private String codigoBarras = "";

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "codigo_interno", nullable = false, length = 10)
    private String codigoInterno;

    @Column(name = "precio_compra", precision = 8, scale = 2, nullable = false)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", precision = 8, scale = 2, nullable = false)
    private BigDecimal precioVenta;

    @Column(name = "estado", nullable = false)
    private Integer estado;

    @Column(name = "marca_baja", nullable = false)
    @Builder.Default
    private Boolean marcaBaja = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", nullable = false)
    private Integer usuarioCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "usuario_modificacion")
    private Integer usuarioModificacion;
}
