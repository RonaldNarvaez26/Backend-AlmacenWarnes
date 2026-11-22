package com.almacen.warnes.modulo_analitica.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarArqueoDTO {

    /**
     * ID de la sesión. Si es nulo, el backend busca la sesión activa del usuario.
     */
    private Integer sesionId;

    /**
     * Monto inicial de caja (opcional, por ahora se envía null).
     */
    private BigDecimal montoBase;

    /**
     * Indicadores congelados del turno (ventas esperadas, declaradas, diferencias, top productos).
     */
    private Object datosSnapshot;

    @Valid
    @Builder.Default
    private List<PartidaArqueoDTO> entradas = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<PartidaArqueoDTO> salidas = new ArrayList<>();

    @Builder.Default
    private String periodo = "Turno";
}
