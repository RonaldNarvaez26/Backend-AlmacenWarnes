package com.almacen.warnes.modulo_inventario.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProveedorListadoProjection {
    Integer getProveedor_id();
    String getNombre_empresa();
    String getNombre_contacto();
    String getNumero_contacto();
    String getAnotaciones();
    Boolean getMarca_baja();
    LocalDateTime getFecha_creacion();
    Long getTotal_productos();
    Long getTotal_lotes();
    Long getLotes_activos();
    Long getLotes_en_riesgo();
    Long getStock_total_acumulado();
    BigDecimal getValor_mercaderia();
}

