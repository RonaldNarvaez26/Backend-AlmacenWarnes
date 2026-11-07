package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de sucursal física")
public class SucursalDTO {

    @Schema(description = "Identificador único de sucursal", example = "1")
    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @Schema(description = "Código interno de sucursal", example = "SUC-001")
    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @Schema(description = "Ciudad de ubicación", example = "Warnes Central")
    @JsonProperty("ciudad")
    private String ciudad;

    @Schema(description = "Dirección física", example = "Av. 25 de Mayo esq. Warnes #450, Zona Central")
    @JsonProperty("direccion")
    private String direccion;

    @Schema(description = "Teléfono de contacto", example = "39223344")
    @JsonProperty("telefono")
    private String telefono;

    @Schema(description = "Marca de baja lógica", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    // Aliases para snake_case y compatibilidad
    @JsonProperty("sucursal_id")
    public Integer getSucursal_id() {
        return sucursalId;
    }

    @JsonProperty("codigo_interno")
    public String getCodigo_interno() {
        return codigoInterno;
    }

    @JsonProperty("codigo")
    public String getCodigo() {
        return codigoInterno;
    }

    @JsonProperty("nombre")
    public String getNombre() {
        return ciudad;
    }

    @JsonProperty("marca_baja")
    public Boolean getMarca_baja() {
        return marcaBaja;
    }
}

