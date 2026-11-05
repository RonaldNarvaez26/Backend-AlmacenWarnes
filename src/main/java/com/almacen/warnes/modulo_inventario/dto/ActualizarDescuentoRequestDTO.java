package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload para activar o desactivar precio de oferta / descuento en un lote de inventario")
public class ActualizarDescuentoRequestDTO {

    @NotNull(message = "El campo enDescuento es obligatorio")
    @Schema(description = "Indica si el lote entra en oferta especial", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("enDescuento")
    @JsonAlias({"en_descuento", "en_oferta", "enOferta"})
    private Boolean enDescuento;

    @DecimalMin(value = "0.00", message = "El precio de descuento no puede ser negativo")
    @Schema(description = "Precio rebajado de venta para acelerar rotación del lote", example = "4.50")
    @JsonProperty("precioDescuento")
    @JsonAlias({"precio_descuento", "precio_oferta", "precioOferta"})
    private BigDecimal precioDescuento;
}
