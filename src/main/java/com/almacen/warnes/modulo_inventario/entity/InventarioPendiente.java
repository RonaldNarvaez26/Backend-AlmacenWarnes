package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventario_pendientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventario_pendiente_id")
    private Integer inventarioPendienteId;

    @Column(name = "sucursal_id", nullable = false)
    @Builder.Default
    private Integer sucursalId = 1;

    @Column(name = "producto_id")
    private Integer productoId;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "codigo_barras", length = 100)
    private String codigoBarras;

    @Column(name = "codigo_interno", nullable = false, length = 10)
    private String codigoInterno;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;

    @Column(name = "precio_compra", precision = 8, scale = 2, nullable = false)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", precision = 8, scale = 2, nullable = false)
    private BigDecimal precioVenta;

    @Column(name = "stock_ingresado", nullable = false)
    private Integer stockIngresado;

    @Column(name = "estado", nullable = false)
    private Integer estado;

    @Column(name = "marca_baja", nullable = false)
    @Builder.Default
    private Boolean marcaBaja = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", nullable = false)
    private Integer usuarioCreacion;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "usuario_revision")
    private Integer usuarioRevision;

    // --- CAMPOS TRANSITORIOS (En tránsito a nivel de DTO / Aplicación, NO existen como columnas en BD) ---

    @Transient
    private Integer proveedorId;

    @Transient
    private String descripcionLote;

    @Transient
    private BigDecimal precioLote;

    @Transient
    private LocalDate fechaVencimiento;
}
