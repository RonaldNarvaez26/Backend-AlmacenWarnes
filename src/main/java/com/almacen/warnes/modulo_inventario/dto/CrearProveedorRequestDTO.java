package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearProveedorRequestDTO {

    @NotBlank(message = "El nombre o razón social de la empresa es obligatorio")
    @Size(max = 50, message = "El nombre de la empresa no puede superar los 50 caracteres")
    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @NotBlank(message = "El nombre del contacto logístico es obligatorio")
    @Size(max = 50, message = "El nombre del contacto no puede superar los 50 caracteres")
    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @NotBlank(message = "El número de contacto es obligatorio")
    @Size(max = 15, message = "El número de contacto no puede superar los 15 caracteres")
    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @Size(max = 200, message = "Las anotaciones contractuales no pueden superar los 200 caracteres")
    @JsonProperty("anotaciones")
    private String anotaciones;

    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;
}

