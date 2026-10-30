package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarDescuentoRequestDTO {

    @NotNull(message = "El campo enDescuento es obligatorio")
    @JsonProperty("enDescuento")
    @JsonAlias({"en_descuento", "en_oferta", "enOferta"})
    private Boolean enDescuento;

    @DecimalMin(value = "0.00", message = "El precio de descuento no puede ser negativo")
    @JsonProperty("precioDescuento")
    @JsonAlias({"precio_descuento", "precio_oferta", "precioOferta"})
    private BigDecimal precioDescuento;
}

