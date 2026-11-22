package com.almacen.warnes.modulo_analitica.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class ModificarMovimientoDTO {

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    private Integer tipoMovimiento;

    private Integer metodoMovimiento;

    @Size(max = 40, message = "El detalle no puede exceder los 40 caracteres")
    private String detalle;
}
