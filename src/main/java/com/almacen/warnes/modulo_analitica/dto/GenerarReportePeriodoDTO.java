package com.almacen.warnes.modulo_analitica.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarReportePeriodoDTO {

    private Integer sucursalId;

    private String periodo;

    @NotNull(message = "La fecha y hora de inicio es obligatoria.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha y hora de fin es obligatoria.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime fechaFin;
}
