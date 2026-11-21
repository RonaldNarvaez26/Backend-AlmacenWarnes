CREATE OR REPLACE FUNCTION fn_reportes_listar(
    p_sucursal_id INT DEFAULT NULL,
    p_periodo VARCHAR DEFAULT NULL,
    p_fecha_desde DATE DEFAULT NULL,
    p_fecha_hasta DATE DEFAULT NULL,
    p_usuario_id INT DEFAULT NULL
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
    usuario_creacion INT,
    nombre_usuario VARCHAR,
    fecha_creacion TIMESTAMP
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
        r.usuario_creacion,
        (u.nombres || ' ' || u.apellidos)::VARCHAR AS nombre_usuario,
        r.fecha_creacion
    FROM Reportes r
    INNER JOIN Sucursales s ON r.sucursal_id = s.sucursal_id
    INNER JOIN Usuarios u   ON r.usuario_creacion = u.usuario_id
    WHERE r.marca_baja = false
      AND (p_sucursal_id IS NULL OR r.sucursal_id = p_sucursal_id)
      AND (p_periodo IS NULL OR r.periodo ILIKE p_periodo)
      AND (p_fecha_desde IS NULL OR r.fecha_inicio::DATE >= p_fecha_desde)
      AND (p_fecha_hasta IS NULL OR r.fecha_fin::DATE <= p_fecha_hasta)
      AND (p_usuario_id IS NULL OR r.usuario_creacion = p_usuario_id)
    ORDER BY r.fecha_creacion DESC;
END;
$$;