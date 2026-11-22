package com.almacen.warnes.modulo_analitica.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class PartidaArqueoDTO {

    /**
     * Código de concepto de pago/ingreso/egreso (Grupo 4: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto, 5=Otro)
     */
    @JsonAlias({"tipoEntrada", "tipoSalida", "tipo_entrada", "tipo_salida"})
    @NotNull(message = "El tipo de partida es obligatorio")
    private Integer tipo;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto de la partida debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "La descripción es obligatoria")
    @Size(max = 40, message = "La descripción no puede exceder los 40 caracteres")
    private String descripcion;
}
