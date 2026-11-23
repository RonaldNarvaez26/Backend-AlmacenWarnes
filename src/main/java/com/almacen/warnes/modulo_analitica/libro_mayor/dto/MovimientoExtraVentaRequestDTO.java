package com.almacen.warnes.modulo_analitica.libro_mayor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoExtraVentaRequestDTO {
    private Integer sucursalId;

    @NotNull(message = "El tipo de movimiento es obligatorio (1 = Entrada, 2 = Salida)")
    private Integer tipoMovimiento;

    private Integer categoriaId;

    @NotNull(message = "El método de pago es obligatorio")
    private Integer metodoPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El detalle es obligatorio")
    @Size(min = 3, max = 40, message = "El detalle debe tener entre 3 y 40 caracteres")
    private String detalle;
}
