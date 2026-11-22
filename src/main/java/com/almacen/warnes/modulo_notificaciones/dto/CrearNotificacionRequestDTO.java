package com.almacen.warnes.modulo_notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearNotificacionRequestDTO {

    private Integer usuarioDestino;

    private Integer rolDestino;

    @NotNull(message = "El tipo de notificación (Grupo 10) es obligatorio")
    private Integer tipoNotificacion;

    @NotBlank(message = "El título de la alerta es obligatorio")
    @Size(max = 100, message = "El título no puede exceder 100 caracteres")
    private String tituloAlerta;

    @NotBlank(message = "El mensaje de la alerta es obligatorio")
    @Size(max = 255, message = "El mensaje no puede exceder 255 caracteres")
    private String mensajeAlerta;

    @NotBlank(message = "La dirección o ruta de acción es obligatoria")
    @Size(max = 100, message = "La dirección no puede exceder 100 caracteres")
    private String direccion;
}
