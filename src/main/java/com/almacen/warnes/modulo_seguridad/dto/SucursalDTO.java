package com.almacen.warnes.modulo_seguridad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para la información de una sucursal física")
public class SucursalDTO {

    @Schema(description = "Identificador único de la sucursal", example = "1")
    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @Schema(description = "Código interno de la sucursal", example = "SUC-001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Ciudad donde se ubica la sucursal", example = "Warnes Central")
    @JsonProperty("ciudad")
    private String ciudad;

    @Schema(description = "Dirección física de la sucursal", example = "Av. 25 de Mayo esq. Warnes #450, Zona Central")
    @JsonProperty("direccion")
    private String direccion;

    @Schema(description = "Teléfono de contacto", example = "39223344")
    @JsonProperty("telefono")
    private String telefono;

    @Schema(description = "Fecha de registro de la sucursal")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Cantidad de usuarios activos asignados a la sucursal", example = "4")
    @JsonProperty("cantidadUsuarios")
    private Integer cantidadUsuarios;

    @Schema(description = "Indica si la sucursal está dada de baja lógica", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    // Aliases para compatibilidad snake_case en clientes o vistas
    @JsonProperty("sucursal_id")
    public Integer getSucursal_id() {
        return sucursalId;
    }

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() {
        return codigoInterno;
    }

    @JsonProperty("cantidad_usuarios")
    public Integer getCantidad_usuarios() {
        return cantidadUsuarios;
    }
}

