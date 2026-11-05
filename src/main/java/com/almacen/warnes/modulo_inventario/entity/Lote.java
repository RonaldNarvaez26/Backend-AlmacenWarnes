package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lote_id")
    private Integer loteId;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_lote", precision = 8, scale = 2, nullable = false)
    private BigDecimal precioLote;

    @Column(name = "costo_unitario", precision = 8, scale = 2, nullable = false)
    private BigDecimal costoUnitario;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "marca_baja", nullable = false)
    @Builder.Default
    private Boolean marcaBaja = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;
}
