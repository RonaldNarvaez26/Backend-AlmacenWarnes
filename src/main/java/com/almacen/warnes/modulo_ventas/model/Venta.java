package com.almacen.warnes.modulo_ventas.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla Ventas.
 * estado     → Conceptos grupo_id=5: 1=Pendiente, 2=Finalizada, 3=Anulada
 * metodoVenta → Conceptos grupo_id=4: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    private Integer ventaId;
    private Integer sucursalId;

    /** Conceptos grupo_id=5: 1=Pendiente, 2=Finalizada, 3=Anulada */
    private Integer estado;

    private BigDecimal precioTotal;

    /** Conceptos grupo_id=4: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto */
    private Integer metodoVenta;

    private BigDecimal cobroQr;
    private BigDecimal cobroEfectivo;
    private BigDecimal cobroTarjeta;

    /** Nullable — cuánto entregó el cliente (relevante solo en efectivo) */
    private BigDecimal montoRecibido;

    /** Nullable — vuelto entregado */
    private BigDecimal cambio;

    /** Nullable — FK a Clientes (venta anónima si null) */
    private Integer clienteId;

    /** Nullable — usuario_id de quien anuló la venta */
    private Integer anuladoPor;

    private Boolean marcaBaja;
    private LocalDateTime fechaCreacion;
    private Integer usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private Integer usuarioModificacion;
}
