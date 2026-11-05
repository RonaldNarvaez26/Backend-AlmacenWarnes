package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventario_id")
    private Integer inventarioId;

    @Column(name = "sucursal_id", nullable = false)
    @Builder.Default
    private Integer sucursalId = 1;

    @Column(name = "lote_id", nullable = false)
    private Integer loteId;

    @Column(name = "producto_id", nullable = false)
    private Integer productoId;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "en_descuento", nullable = false)
    @Builder.Default
    private Boolean enDescuento = false;

    @Column(name = "precio_descuento", precision = 8, scale = 2)
    private BigDecimal precioDescuento;

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
