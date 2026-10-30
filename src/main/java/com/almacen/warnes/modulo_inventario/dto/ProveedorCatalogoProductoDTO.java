package com.almacen.warnes.modulo_inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorCatalogoProductoDTO {

    @JsonProperty("productoId")
    private Integer productoId;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigoBarras")
    private String codigoBarras;

    @JsonProperty("nombreProducto")
    private String nombreProducto;

    @JsonProperty("categoriaNombre")
    private String categoriaNombre;

    @JsonProperty("precioCompra")
    private BigDecimal precioCompra;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("estadoProducto")
    private Integer estadoProducto;

    @JsonProperty("stockActual")
    private Long stockActual;

    @JsonProperty("totalLotesRecibidos")
    private Long totalLotesRecibidos;

    @JsonProperty("fechaUltimaEntrega")
    private LocalDateTime fechaUltimaEntrega;
}

