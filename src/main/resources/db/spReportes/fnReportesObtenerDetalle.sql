CREATE OR REPLACE FUNCTION fn_reportes_obtener_detalle(
    p_reporte_id INT
)
RETURNS TABLE (
    reporte_id INT,
    sucursal_id INT,
    codigo_sucursal VARCHAR,
    periodo VARCHAR,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    monto_base DECIMAL(8,2),
    monto_entradas DECIMAL(8,2),
    monto_salidas DECIMAL(8,2),
    utilidad DECIMAL(8,2),
    datos_snapshot JSONB,
    nombre_usuario VARCHAR,
    fecha_creacion TIMESTAMP,
    entradas JSONB,
    salidas JSONB,
    movimientos JSONB
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.reporte_id,
        r.sucursal_id,
        s.codigo_interno::VARCHAR AS codigo_sucursal,
        r.periodo::VARCHAR,
        r.fecha_inicio,
        r.fecha_fin,
        r.monto_base,
        r.monto_entradas,
        r.monto_salidas,
        r.utilidad,
        r.datos_snapshot,
        (u.nombres || ' ' || u.apellidos)::VARCHAR AS nombre_usuario,
        r.fecha_creacion,

        COALESCE(
            (SELECT jsonb_agg(
                jsonb_build_object(
                    'entrada_id', e.entrada_id,
                    'tipo_entrada', e.tipo_entrada,
                    'descripcion_tipo', ce.descripcion,
                    'monto', e.monto,
                    'descripcion', e.descripcion
                ) ORDER BY e.entrada_id
            )
            FROM Entradas e
            LEFT JOIN Conceptos ce
                   ON e.tipo_entrada = ce.codigo AND ce.grupo_id = 4 AND ce.marca_baja = false
            WHERE e.reporte_id = r.reporte_id AND e.marca_baja = false),
            '[]'::JSONB
        ) AS entradas,

        COALESCE(
            (SELECT jsonb_agg(
                jsonb_build_object(
                    'salida_id', sa.salida_id,
                    'tipo_salida', sa.tipo_salida,
                    'descripcion_tipo', cs.descripcion,
                    'monto', sa.monto,
                    'descripcion', sa.descripcion
                ) ORDER BY sa.salida_id
            )
            FROM Salidas sa
            LEFT JOIN Conceptos cs
                   ON sa.tipo_salida = cs.codigo AND cs.grupo_id = 4 AND cs.marca_baja = false
            WHERE sa.reporte_id = r.reporte_id AND sa.marca_baja = false),
            '[]'::JSONB
        ) AS salidas,

        COALESCE(
            (SELECT jsonb_agg(
                jsonb_build_object(
                    'movimiento_id', m.movimiento_id,
                    'tipo_movimiento', m.tipo_movimiento,
                    'descripcion_tipo', ct.descripcion,
                    'metodo_movimiento', m.metodo_movimiento,
                    'descripcion_metodo', cm.descripcion,
                    'monto', m.monto,
                    'detalle', m.detalle,
                    'fecha_creacion', m.fecha_creacion
                ) ORDER BY m.fecha_creacion
            )
            FROM Movimientos m
            LEFT JOIN Conceptos ct ON m.tipo_movimiento   = ct.codigo AND ct.grupo_id = 6 AND ct.marca_baja = false
            LEFT JOIN Conceptos cm ON m.metodo_movimiento = cm.codigo AND cm.grupo_id = 4 AND cm.marca_baja = false
            WHERE m.reporte_id = r.reporte_id AND m.marca_baja = false),
            '[]'::JSONB
        ) AS movimientos

    FROM Reportes r
    INNER JOIN Sucursales s ON r.sucursal_id = s.sucursal_id
    INNER JOIN Usuarios u   ON r.usuario_creacion = u.usuario_id
    WHERE r.reporte_id = p_reporte_id
      AND r.marca_baja = false;
END;
$$;