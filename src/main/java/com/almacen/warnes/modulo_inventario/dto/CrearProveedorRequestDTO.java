package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para registrar un nuevo distribuidor comercial")
public class CrearProveedorRequestDTO {

    @NotBlank(message = "El nombre o razón social de la empresa es obligatorio")
    @Size(max = 50, message = "El nombre de la empresa no puede superar los 50 caracteres")
    @Schema(description = "Razón social o nombre comercial de la empresa distribuidora", example = "PIL Andina S.A.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;

    @NotBlank(message = "El nombre del contacto logístico es obligatorio")
    @Size(max = 50, message = "El nombre del contacto no puede superar los 50 caracteres")
    @Schema(description = "Nombre completo del contacto logístico o preventista", example = "Roberto Gómez", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    @JsonProperty("nombreContacto")
    private String nombreContacto;

    @NotBlank(message = "El número de contacto es obligatorio")
    @Size(max = 15, message = "El número de contacto no puede superar los 15 caracteres")
    @Schema(description = "Número telefónico o de celular del contacto", example = "70112233", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 15)
    @JsonProperty("numeroContacto")
    private String numeroContacto;

    @Size(max = 200, message = "Las anotaciones contractuales no pueden superar los 200 caracteres")
    @Schema(description = "Acuerdos y términos contractuales vigentes (días de visita, crédito, bonificaciones)", example = "Contrato CONTR-PIL-2026: Entregas martes y viernes 08:00 AM. Crédito quincenal.", maxLength = 200)
    @JsonProperty("anotaciones")
    private String anotaciones;

    @Schema(description = "Identificador del usuario que registra el distribuidor (por defecto 1)", example = "1")
    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;
}
