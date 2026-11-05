package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @Column(name = "nombre_empresa", nullable = false, length = 50)
    private String nombreEmpresa;

    @Column(name = "nombre_contacto", nullable = false, length = 50)
    private String nombreContacto;

    @Column(name = "numero_contacto", nullable = false, length = 15)
    private String numeroContacto;

    @Column(name = "anotaciones", nullable = false, length = 200)
    private String anotaciones;

    @Column(name = "marca_baja", nullable = false)
    @Builder.Default
    private Boolean marcaBaja = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", nullable = false)
    private Integer usuarioCreacion;
}
