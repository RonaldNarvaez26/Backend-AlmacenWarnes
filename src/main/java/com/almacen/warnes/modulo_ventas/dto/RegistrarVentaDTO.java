package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para registrar una venta completa.
 * Mapea directamente a los parámetros de sp_ventas_registrar.
 *
 * NOTA ARQUITECTURAL: El frontend envía este DTO solo cuando los 5 segundos
 * de cuenta regresiva expiran sin que el cajero presione "Abortar".
 * Nunca se envía antes, así se evita saturar la BD con ventas que se anulan inmediatamente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para registrar una venta en la terminal POS")
public class RegistrarVentaDTO {

    @NotNull(message = "sucursalId es obligatorio")
    @Schema(description = "ID de la sucursal donde se realiza la venta", example = "1")
    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @NotNull(message = "usuarioId (cajero) es obligatorio")
    @Schema(description = "ID del cajero que realiza la venta", example = "3")
    @JsonProperty("usuarioId")
    private Integer usuarioId;

    @Schema(description = "ID del cliente (nullable — null para ventas anónimas)", example = "null")
    @JsonProperty("clienteId")
    private Integer clienteId;

    @NotNull(message = "metodoVenta es obligatorio")
    @Schema(description = "Método de pago: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto (Conceptos grupo_id=4)", example = "1")
    @JsonProperty("metodoVenta")
    private Integer metodoVenta;

    @NotNull(message = "precioTotal es obligatorio")
    @Positive(message = "precioTotal debe ser positivo")
    @Schema(description = "Suma total de la venta", example = "21.00")
    @JsonProperty("precioTotal")
    private BigDecimal precioTotal;

    @Schema(description = "Monto cobrado en efectivo (0 si no aplica)", example = "21.00")
    @JsonProperty("cobroEfectivo")
    @Builder.Default
    private BigDecimal cobroEfectivo = BigDecimal.ZERO;

    @Schema(description = "Monto cobrado por QR (0 si no aplica)", example = "0")
    @JsonProperty("cobroQr")
    @Builder.Default
    private BigDecimal cobroQr = BigDecimal.ZERO;

    @Schema(description = "Monto cobrado con tarjeta (0 si no aplica)", example = "0")
    @JsonProperty("cobroTarjeta")
    @Builder.Default
    private BigDecimal cobroTarjeta = BigDecimal.ZERO;

    @Schema(description = "Cuánto entregó el cliente (nullable — solo relevante en efectivo)", example = "25.00")
    @JsonProperty("montoRecibido")
    private BigDecimal montoRecibido;

    @Schema(description = "Vuelto entregado (nullable — 0 en QR/Tarjeta)", example = "4.00")
    @JsonProperty("cambio")
    private BigDecimal cambio;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    @Schema(description = "Lista de ítems del carrito")
    @JsonProperty("detalles")
    private List<DetalleVentaDTO> detalles;

    /** Duración en segundos del cronómetro de atención (métrica) */
    @Schema(description = "Duración en segundos del cronómetro de atención (inicio al escanear 1er producto)", example = "45")
    @JsonProperty("duracionAtencionSegundos")
    private Integer duracionAtencionSegundos;
}

