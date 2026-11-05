package com.almacen.warnes.modulo_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conceptos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concepto {

    @Id
    @Column(name = "concepto_id")
    private Integer conceptoId;

    @Column(name = "grupo_id", nullable = false)
    private Integer grupoId;

    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @Column(name = "descripcion", nullable = false, length = 80)
    private String descripcion;

    @Column(name = "marca_baja", nullable = false)
    @Builder.Default
    private Boolean marcaBaja = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", nullable = false)
    private Integer usuarioCreacion;
}

