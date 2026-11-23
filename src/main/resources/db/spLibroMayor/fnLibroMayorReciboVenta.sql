-- ============================================================================================
-- AUTOR:           Antigravity / Almacen Warnes
-- DESCRIPCION:     OBTIENE DETALLE COMPLETO DE VENTA Y PRODUCTOS PARA EMISION DE RECIBO PDF
-- ============================================================================================
DROP FUNCTION IF EXISTS fn_libro_mayor_recibo_venta(INT);

CREATE OR REPLACE FUNCTION fn_libro_mayor_recibo_venta(p_venta_id INT)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_recibo JSONB;
BEGIN
    SELECT jsonb_build_object(
        'ventaId', v.venta_id,
        'codigoRecibo', ('REC-VNT-' || LPAD(v.venta_id::text, 6, '0')),
        'fechaHora', v.fecha_creacion,
        'sucursalId', v.sucursal_id,
        'sucursalNombre', COALESCE(s.codigo_interno || ' (' || s.ciudad || ')', 'Sucursal #' || v.sucursal_id),
        'sucursalDireccion', COALESCE(s.direccion, 'Dirección Central Warnes'),
        'sucursalTelefono', COALESCE(s.telefono, '39223344'),
        'cajeroId', v.usuario_creacion,
        'cajeroNombre', COALESCE(u.nombres || ' ' || u.apellidos, u.usuario, 'Usuario #' || v.usuario_creacion),
        'cajeroUsuario', COALESCE(u.usuario, ''),
        'clienteId', v.cliente_id,
        'clienteNombre', COALESCE(cl.nombres || ' ' || cl.apellidos, cl.nombre_razon_social, 'Sin Nombre / Control Tributario'),
        'clienteRazonSocial', COALESCE(cl.nombre_razon_social, 'Público General'),
        'clienteNit', COALESCE(cl.nit, '0'),
        'clienteTelefono', COALESCE(cl.telefono, '-'),
        'metodoVentaId', v.metodo_venta,
        'metodoVentaNombre', COALESCE(cm.descripcion, 'Efectivo'),
        'cobroEfectivo', v.cobro_efectivo,
        'cobroQr', v.cobro_qr,
        'cobroTarjeta', v.cobro_tarjeta,
        'montoRecibido', v.monto_recibido,
        'cambio', v.cambio,
        'precioTotal', v.precio_total,
        'estadoId', v.estado,
        'estadoNombre', COALESCE(ce.descripcion, 'Finalizada'),
        'marcaBaja', (v.marca_baja OR v.estado = 3),
        'anuladoPor', v.anulado_por,
        'anuladoPorNombre', COALESCE(ua.nombres || ' ' || ua.apellidos, ''),
        'detalles', COALESCE(
            (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'detalleVentaId', dv.detalle_venta_id,
                        'productoId', dv.producto_id,
                        'codigoInterno', p.codigo_interno,
                        'codigoBarras', p.codigo_barras,
                        'nombreProducto', p.nombre_producto,
                        'cantidad', dv.cantidad,
                        'precioUnitario', dv.precio_unitario,
                        'subtotal', (dv.cantidad * dv.precio_unitario)
                    ) ORDER BY dv.detalle_venta_id ASC
                )
                FROM Detalles_Ventas dv
                JOIN Productos p ON dv.producto_id = p.producto_id
                WHERE dv.venta_id = v.venta_id
            ),
            '[]'::jsonb
        )
    ) INTO v_recibo
    FROM Ventas v
    LEFT JOIN Sucursales s ON v.sucursal_id = s.sucursal_id
    LEFT JOIN Usuarios u ON v.usuario_creacion = u.usuario_id
    LEFT JOIN Usuarios ua ON v.anulado_por = ua.usuario_id
    LEFT JOIN Clientes cl ON v.cliente_id = cl.cliente_id
    LEFT JOIN Conceptos cm ON v.metodo_venta = cm.codigo AND cm.grupo_id = 4 AND cm.marca_baja = false
    LEFT JOIN Conceptos ce ON v.estado = ce.codigo AND ce.grupo_id = 5 AND ce.marca_baja = false
    WHERE v.venta_id = p_venta_id;

    RETURN v_recibo;
END;
$$;
