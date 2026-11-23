package com.almacen.warnes.modulo_analitica.libro_mayor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltrosOpcionesDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CajeroOpcionDTO {
        private Integer usuarioId;
        private String usuario;
        private String nombreCompleto;
        private Integer sucursalId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoOpcionDTO {
        private Integer productoId;
        private String codigoInterno;
        private String nombreProducto;
    }

    private List<CajeroOpcionDTO> cajeros;
    private List<ProductoOpcionDTO> productos;
}
