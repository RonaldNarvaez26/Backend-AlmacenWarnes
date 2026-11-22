package com.almacen.warnes.modulo_analitica.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearMovimientoDTO {

    /**
     * Requerido para Propietarios (rol 3). Opcional para Cajeros/Supervisores (se toma de su usuario/token).
     */
    private Integer sucursalId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El tipo de movimiento es obligatorio (1=Entrada, 2=Salida)")
    private Integer tipoMovimiento;

    @NotNull(message = "El método de movimiento es obligatorio (1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto, 5=Otro)")
    private Integer metodoMovimiento;

    @NotNull(message = "El detalle es obligatorio")
    @Size(max = 40, message = "El detalle no puede exceder los 40 caracteres")
    private String detalle;
}
