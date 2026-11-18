package com.almacen.warnes.modulo_notificaciones.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Column(name = "usuario_destino")
    private Integer usuarioDestino;

    @Column(name = "rol_destino")
    private Integer rolDestino;

    @Column(name = "tipo_notificacion", nullable = false)
    private Integer tipoNotificacion;

    @Column(name = "titulo_alerta", nullable = false, length = 100)
    private String tituloAlerta;

    @Column(name = "mensaje_alerta", nullable = false, length = 255)
    private String mensajeAlerta;

    @Column(name = "direccion", nullable = false, length = 100)
    private String direccion;

    @Column(name = "leido", nullable = false)
    private Boolean leido;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_leido")
    private LocalDateTime fechaLeido;

    @PrePersist
    public void prePersist() {
        if (this.leido == null) {
            this.leido = false;
        }
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
    }
}
