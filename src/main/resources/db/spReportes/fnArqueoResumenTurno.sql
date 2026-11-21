-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     RESUMEN DEL TURNO ACTIVO DEL CAJERO (VENTAS + MOVIMIENTOS + FLAG ARQUEO)
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_arqueo_resumen_turno(
    p_usuario_id INT
)
RETURNS TABLE (
    sesion_id INT,
    sucursal_id INT,
    hora_inicio_turno TIMESTAMP,
    conteo_ventas INT,
    total_ventas_efectivo NUMERIC(12,2),
    total_ventas_qr NUMERIC(12,2),
    total_ventas_tarjeta NUMERIC(12,2),
    total_ventas_general NUMERIC(12,2),
    total_movimientos_entrada NUMERIC(12,2),
    total_movimientos_salida NUMERIC(12,2),
    cantidad_movimientos INT,
    requiere_arqueo BOOLEAN,
    movimientos_detalle JSONB,
    top_productos JSONB
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    WITH sesion_activa AS (
        SELECT
            s.sesion_id,
            u.sucursal_id,
            s.hora_sesion_abierta
        FROM Sesiones s
        INNER JOIN Usuarios u ON s.usuario_id = u.usuario_id
        WHERE s.usuario_id = p_usuario_id
          AND s.hora_sesion_cerrada IS NULL
          AND s.marca_baja = false
        ORDER BY s.hora_sesion_abierta DESC
        LIMIT 1
    ),
    ventas_turno AS (
        SELECT
            COUNT(*)::INT                                     AS conteo,
            COALESCE(SUM(v.cobro_efectivo), 0)::NUMERIC(12,2) AS tot_efectivo,
            COALESCE(SUM(v.cobro_qr), 0)::NUMERIC(12,2)       AS tot_qr,
            COALESCE(SUM(v.cobro_tarjeta), 0)::NUMERIC(12,2)  AS tot_tarjeta,
            COALESCE(SUM(v.precio_total), 0)::NUMERIC(12,2)   AS tot_general
        FROM Ventas v
        CROSS JOIN sesion_activa sa
        WHERE v.usuario_creacion = p_usuario_id
          AND v.estado = 2 -- Finalizada / Aprobada
          AND v.marca_baja = false
          AND v.fecha_creacion BETWEEN sa.hora_sesion_abierta AND CURRENT_TIMESTAMP
    ),
    movs_filtrados AS (
        SELECT
            m.movimiento_id,
            m.tipo_movimiento,
            ct.descripcion AS descripcion_tipo,
            m.metodo_movimiento,
            cm.descripcion AS descripcion_metodo,
            m.monto,
            m.detalle,
            m.fecha_creacion
        FROM Movimientos m
        CROSS JOIN sesion_activa sa
        LEFT JOIN Conceptos ct ON m.tipo_movimiento   = ct.codigo AND ct.grupo_id = 6 AND ct.marca_baja = false
        LEFT JOIN Conceptos cm ON m.metodo_movimiento = cm.codigo AND cm.grupo_id = 4 AND cm.marca_baja = false
        WHERE m.usuario_creacion = p_usuario_id
          AND m.reporte_id IS NULL
          AND m.marca_baja = false
          AND m.fecha_creacion BETWEEN sa.hora_sesion_abierta AND CURRENT_TIMESTAMP
    ),
    movs_resumen AS (
        SELECT
            COALESCE(SUM(CASE WHEN tipo_movimiento = 1 THEN monto ELSE 0 END), 0)::NUMERIC(12,2) AS tot_entradas,
            COALESCE(SUM(CASE WHEN tipo_movimiento = 2 THEN monto ELSE 0 END), 0)::NUMERIC(12,2) AS tot_salidas,
            COUNT(*)::INT AS cant_movimientos,
            COALESCE(
                jsonb_agg(
                    jsonb_build_object(
                        'movimiento_id', movimiento_id,
                        'tipo_movimiento', tipo_movimiento,
                        'descripcion_tipo', descripcion_tipo,
                        'metodo_movimiento', metodo_movimiento,
                        'descripcion_metodo', descripcion_metodo,
                        'monto', monto,
                        'detalle', detalle,
                        'fecha_creacion', fecha_creacion
                    ) ORDER BY fecha_creacion ASC
                ),
                '[]'::JSONB
            ) AS detalle_json
        FROM movs_filtrados
    ),
    ranking_productos AS (
        SELECT
            p.nombre_producto AS producto,
            SUM(dv.cantidad) AS cantidad,
            SUM(dv.cantidad * dv.precio_unitario) AS subtotal
        FROM Detalles_Ventas dv
        INNER JOIN Ventas v    ON dv.venta_id = v.venta_id
        INNER JOIN Productos p ON dv.producto_id = p.producto_id
        CROSS JOIN sesion_activa sa
        WHERE v.usuario_creacion = p_usuario_id
          AND v.estado = 2
          AND v.marca_baja = false
          AND dv.marca_baja = false
          AND v.fecha_creacion BETWEEN sa.hora_sesion_abierta AND CURRENT_TIMESTAMP
        GROUP BY p.producto_id, p.nombre_producto
        ORDER BY SUM(dv.cantidad) DESC
        LIMIT 5
    )
    SELECT
        sa.sesion_id,
        sa.sucursal_id,
        sa.hora_sesion_abierta AS hora_inicio_turno,
        vt.conteo              AS conteo_ventas,
        vt.tot_efectivo        AS total_ventas_efectivo,
        vt.tot_qr              AS total_ventas_qr,
        vt.tot_tarjeta         AS total_ventas_tarjeta,
        vt.tot_general         AS total_ventas_general,
        mr.tot_entradas        AS total_movimientos_entrada,
        mr.tot_salidas         AS total_movimientos_salida,
        mr.cant_movimientos    AS cantidad_movimientos,
        (vt.conteo > 0 OR mr.cant_movimientos > 0) AS requiere_arqueo,
        mr.detalle_json        AS movimientos_detalle,
        COALESCE(
            (SELECT jsonb_agg(
                jsonb_build_object(
                    'producto', rp.producto,
                    'cantidad', rp.cantidad,
                    'subtotal', rp.subtotal
                )
            ) FROM ranking_productos rp),
            '[]'::JSONB
        ) AS top_productos
    FROM sesion_activa sa
    CROSS JOIN ventas_turno vt
    CROSS JOIN movs_resumen mr;
END;
$$;