package com.almacen.warnes.modulo_ventas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta al registrar o consultar una venta.
 * Incluye el reciboHtml generado por VentaMapper para imprimir en impresora térmica.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle completo de una venta registrada, incluyendo el recibo listo para imprimir")
public class VentaResponseDTO {

    @Schema(description = "ID de la venta registrada", example = "1234")
    @JsonProperty("ventaId")
    private Integer ventaId;

    @JsonProperty("sucursalId")
    private Integer sucursalId;

    @Schema(description = "Nombre de la sucursal (enriquecido por el mapper)")
    @JsonProperty("sucursalNombre")
    private String sucursalNombre;

    @Schema(description = "Estado: 1=Pendiente, 2=Finalizada, 3=Anulada (Conceptos grupo_id=5)", example = "2")
    @JsonProperty("estado")
    private Integer estado;

    @JsonProperty("precioTotal")
    private BigDecimal precioTotal;

    @Schema(description = "Método de pago: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto (Conceptos grupo_id=4)", example = "1")
    @JsonProperty("metodoVenta")
    private Integer metodoVenta;

    @JsonProperty("cobroEfectivo")
    private BigDecimal cobroEfectivo;

    @JsonProperty("cobroQr")
    private BigDecimal cobroQr;

    @JsonProperty("cobroTarjeta")
    private BigDecimal cobroTarjeta;

    @Schema(description = "Nullable — cuánto entregó el cliente")
    @JsonProperty("montoRecibido")
    private BigDecimal montoRecibido;

    @Schema(description = "Nullable — vuelto entregado")
    @JsonProperty("cambio")
    private BigDecimal cambio;

    @Schema(description = "Nullable — ID del cliente si se registró")
    @JsonProperty("clienteId")
    private Integer clienteId;

    @Schema(description = "Nullable — ID del usuario que anuló la venta")
    @JsonProperty("anuladoPor")
    private Integer anuladoPor;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Nombre completo del cajero (enriquecido por el mapper)")
    @JsonProperty("cajeroNombre")
    private String cajeroNombre;

    @JsonProperty("detalles")
    private List<DetalleVentaDTO> detalles;

    @Schema(description = "HTML del recibo térmico 80mm generado por el backend. Vue lo inyecta y ejecuta window.print().")
    @JsonProperty("reciboHtml")
    private String reciboHtml;
}
