package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {

    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @JsonProperty("anotaciones")
    private String anotaciones;

    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @JsonProperty("proveedor_id")
    public Integer getProveedor_id() { return proveedorId; }

    @JsonProperty("nombre_empresa")
    public String getNombre_empresa() { return nombreEmpresa; }

    @JsonProperty("nombre_contacto")
    public String getNombre_contacto() { return nombreContacto; }

    @JsonProperty("numero_contacto")
    public String getNumero_contacto() { return numeroContacto; }

    @JsonProperty("marca_baja")
    public Boolean getMarca_baja() { return marcaBaja; }
}

