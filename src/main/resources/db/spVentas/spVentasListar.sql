-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez / Almacen Warnes
-- DESCRIPCION:     LISTA EL HISTORIAL DE VENTAS POR SUCURSAL CON FILTROS OPCIONALES
--                  Y CONSOLIDA SUS DETALLES EN FORMATO JSON
-- ============================================================================================
CREATE OR REPLACE FUNCTION sp_ventas_listar(
    p_sucursal_id INT,
    p_fecha DATE DEFAULT NULL,
    p_metodo_venta INT DEFAULT NULL,
    p_estado INT DEFAULT NULL
)
RETURNS TABLE (
    venta_id INT,
    sucursal_id INT,
    sucursal_nombre VARCHAR,
    estado INT,
    estado_nombre VARCHAR,
    precio_total DECIMAL(8,2),
    metodo_venta INT,
    metodo_venta_nombre VARCHAR,
    cobro_efectivo DECIMAL(8,2),
    cobro_qr DECIMAL(8,2),
    cobro_tarjeta DECIMAL(8,2),
    monto_recibido DECIMAL(8,2),
    cambio DECIMAL(8,2),
    cliente_id INT,
    cliente_nombre VARCHAR,
    cliente_nit VARCHAR,
    anulado_por INT,
    anulado_por_nombre VARCHAR,
    fecha_creacion TIMESTAMP,
    usuario_creacion INT,
    cajero_nombre VARCHAR,
    detalles_json JSON
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        v.venta_id,
        v.sucursal_id,
        COALESCE(s.codigo_interno || ' - ' || s.ciudad, 'Sucursal #' || v.sucursal_id)::VARCHAR AS sucursal_nombre,
        v.estado,
        COALESCE(ce.descripcion, 'Desconocido')::VARCHAR AS estado_nombre,
        v.precio_total,
        v.metodo_venta,
        COALESCE(cm.descripcion, 'Desconocido')::VARCHAR AS metodo_venta_nombre,
        v.cobro_efectivo,
        v.cobro_qr,
        v.cobro_tarjeta,
        v.monto_recibido,
        v.cambio,
        v.cliente_id,
        COALESCE(c.nombre_razon_social, NULLIF(TRIM(c.nombres || ' ' || COALESCE(c.apellidos, '')), ''), 'Consumidor Final')::VARCHAR AS cliente_nombre,
        COALESCE(c.nit, '0')::VARCHAR AS cliente_nit,
        v.anulado_por,
        COALESCE(TRIM(ua.nombres || ' ' || COALESCE(ua.apellidos, '')), ua.usuario, '')::VARCHAR AS anulado_por_nombre,
        v.fecha_creacion,
        v.usuario_creacion,
        COALESCE(TRIM(uc.nombres || ' ' || COALESCE(uc.apellidos, '')), uc.usuario, 'Cajero #' || v.usuario_creacion)::VARCHAR AS cajero_nombre,
        COALESCE(
            (
                SELECT json_agg(
                    json_build_object(
                        'detalle_venta_id', dv.detalle_venta_id,
                        'producto_id', dv.producto_id,
                        'inventario_id', dv.inventario_id,
                        'nombre_producto', p.nombre_producto,
                        'codigo_interno', p.codigo_interno,
                        'codigo_barras', p.codigo_barras,
                        'precio_unitario', dv.precio_unitario,
                        'cantidad', dv.cantidad,
                        'subtotal', (dv.cantidad * dv.precio_unitario)
                    )
                )
                FROM Detalles_Ventas dv
                INNER JOIN Productos p ON dv.producto_id = p.producto_id
                WHERE dv.venta_id = v.venta_id AND dv.marca_baja = false
            ),
            '[]'::json
        ) AS detalles_json
    FROM Ventas v
    INNER JOIN Sucursales s ON v.sucursal_id = s.sucursal_id
    INNER JOIN Usuarios uc ON v.usuario_creacion = uc.usuario_id
    LEFT JOIN Usuarios ua ON v.anulado_por = ua.usuario_id
    LEFT JOIN Clientes c ON v.cliente_id = c.cliente_id
    LEFT JOIN Conceptos ce ON ce.grupo_id = 5 AND ce.codigo = v.estado
    LEFT JOIN Conceptos cm ON cm.grupo_id = 4 AND cm.codigo = v.metodo_venta
    WHERE v.sucursal_id = p_sucursal_id
      AND v.marca_baja = false
      AND (p_fecha IS NULL OR DATE(v.fecha_creacion) = p_fecha)
      AND (p_metodo_venta IS NULL OR v.metodo_venta = p_metodo_venta)
      AND (p_estado IS NULL OR v.estado = p_estado)
    ORDER BY v.fecha_creacion DESC;
END;
$$;
