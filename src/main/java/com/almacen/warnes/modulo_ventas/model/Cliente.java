package com.almacen.warnes.modulo_ventas.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla Clientes.
 * La relación Ventas.cliente_id es nullable — la mayoría de ventas son anónimas.
 * Se usa cuando el cliente necesita factura con NIT/CI o razón social.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    private Integer clienteId;

    /** NIT o CI para facturación — varchar(20) */
    private String nit;

    private String nombres;
    private String apellidos;

    /** Para facturación empresarial */
    private String nombreRazonSocial;

    private String telefono;
    private Boolean marcaBaja;
    private LocalDateTime fechaCreacion;
    private Integer usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private Integer usuarioModificacion;
}

