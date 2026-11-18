package com.almacen.warnes.modulo_notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionItemDTO {
    private String idNotificacion;
    private String tipoAlerta;
    private String severidad;
    private String titulo;
    private String mensaje;
    private Integer sucursalId;
    private String codigoSucursal;
    private Integer referenciaId;
    private String rutaAccion;
    private String direccion;
    private LocalDateTime fechaEvento;
    private Boolean leido;
    private LocalDateTime fechaLeido;
    private Integer tipoNotificacion;
}
