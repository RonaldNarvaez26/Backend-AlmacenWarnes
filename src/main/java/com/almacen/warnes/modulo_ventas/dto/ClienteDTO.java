package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para datos del cliente (tabla Clientes).
 * Ventas.cliente_id es nullable — solo se usa cuando el cliente pide factura/recibo con sus datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del cliente para facturación/recibo")
public class ClienteDTO {

    @Schema(description = "ID del cliente (null al crear uno nuevo)", example = "null")
    @JsonProperty("clienteId")
    private Integer clienteId;

    @Schema(description = "NIT o CI del cliente — varchar(20)", example = "12345678")
    @JsonProperty("nit")
    private String nit;

    @Schema(description = "Nombres del cliente", example = "Juan")
    @JsonProperty("nombres")
    private String nombres;

    @Schema(description = "Apellidos del cliente", example = "Flores García")
    @JsonProperty("apellidos")
    private String apellidos;

    @Schema(description = "Razón social para facturación empresarial", example = "Tienda Flores S.R.L.")
    @JsonProperty("nombreRazonSocial")
    private String nombreRazonSocial;

    @Schema(description = "Teléfono de contacto", example = "70123456")
    @JsonProperty("telefono")
    private String telefono;

    @Schema(description = "Estado de baja lógica", example = "false")
    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @Schema(description = "Fecha de registro del cliente")
    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de última modificación del cliente")
    @JsonProperty("fechaModificacion")
    private LocalDateTime fechaModificacion;

    @Schema(description = "Cantidad total de compras realizadas", example = "12")
    @JsonProperty("totalCompras")
    private Long totalCompras;

    @Schema(description = "Monto total acumulado de compras realizadas", example = "1450.50")
    @JsonProperty("montoTotalComprado")
    private BigDecimal montoTotalComprado;
}
