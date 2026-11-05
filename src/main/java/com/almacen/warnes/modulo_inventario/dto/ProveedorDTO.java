package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos básicos del distribuidor comercial")
public class ProveedorDTO {

    @Schema(description = "Identificador único del proveedor", example = "1")
    @JsonProperty("proveedorId")
    private Integer proveedorId;

    @Schema(description = "Razón social de la empresa", example = "PIL Andina S.A.")
    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @Schema(description = "Contacto logístico o preventista", example = "Roberto Gómez")
    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @Schema(description = "Número telefónico del contacto", example = "70112233")
    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @Schema(description = "Anotaciones contractuales vigentes", example = "Contrato CONTR-PIL-2026: Entregas martes y viernes 08:00 AM. Crédito quincenal.")
    @JsonProperty("anotaciones")
    private String anotaciones;

    @Schema(description = "Bandera de baja lógica", example = "false")
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
