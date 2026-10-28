package com.almacen.warnes.modulo_seguridad.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "rol_id", nullable = false)
    private Integer rolId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "marca_baja", nullable = false)
    private Boolean marcaBaja;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", nullable = false)
    private Integer usuarioCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    @Column(name = "usuario_modificacion", nullable = false)
    private Integer usuarioModificacion;
}
