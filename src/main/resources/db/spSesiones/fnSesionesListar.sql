DROP FUNCTION IF EXISTS fn_sesiones_listar(INT, INT, BOOLEAN, DATE, DATE, VARCHAR);

CREATE OR REPLACE FUNCTION fn_sesiones_listar(
    p_usuario_id INT DEFAULT NULL,
    p_sucursal_id INT DEFAULT NULL,
    p_solo_activas BOOLEAN DEFAULT NULL,
    p_fecha_desde DATE DEFAULT NULL,
    p_fecha_hasta DATE DEFAULT NULL,
    p_texto_busqueda VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    sesion_id INT,
    usuario_id INT,
    usuario VARCHAR,
    nombre_completo VARCHAR,
    descripcion_rol VARCHAR,
    sucursal_id INT,
    codigo_sucursal VARCHAR,
    hora_sesion_abierta TIMESTAMP,
    hora_sesion_cerrada TIMESTAMP,
    duracion_sesion INT,
    esta_activa BOOLEAN,
    reporte_id INT,
    estado_arqueo VARCHAR,
    alerta_sin_arqueo BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.sesion_id,
        s.usuario_id,
        u.usuario::VARCHAR,
        (u.nombres || ' ' || u.apellidos)::VARCHAR AS nombre_completo,
        c.descripcion::VARCHAR AS descripcion_rol,
        u.sucursal_id,
        suc.codigo_interno::VARCHAR AS codigo_sucursal,
        s.hora_sesion_abierta,
        s.hora_sesion_cerrada,
        s.duracion_sesion,
        (s.hora_sesion_cerrada IS NULL) AS esta_activa,
        r.reporte_id,
        (
            CASE
                WHEN s.hora_sesion_cerrada IS NULL THEN 'Turno en curso'
                WHEN r.reporte_id IS NOT NULL      THEN 'Cerrada con arqueo'
                WHEN u.rol_id = 1 AND act.tuvo_actividad THEN 'Cerrada sin arqueo (con actividad)'
                ELSE 'Cerrada sin actividad'
            END
        )::VARCHAR AS estado_arqueo,
        (
            s.hora_sesion_cerrada IS NOT NULL
            AND r.reporte_id IS NULL
            AND u.rol_id = 1 -- Solo alerta sobre Cajeros
            AND act.tuvo_actividad
        ) AS alerta_sin_arqueo
    FROM Sesiones s
    INNER JOIN Usuarios u ON s.usuario_id = u.usuario_id
    LEFT JOIN Conceptos c
           ON u.rol_id = c.codigo AND c.grupo_id = 1 AND c.marca_baja = false
    LEFT JOIN Sucursales suc
           ON u.sucursal_id = suc.sucursal_id AND suc.marca_baja = false
    LEFT JOIN Reportes r
           ON r.usuario_creacion = s.usuario_id
          AND (r.fecha_inicio = s.hora_sesion_abierta OR date_trunc('second', r.fecha_inicio) = date_trunc('second', s.hora_sesion_abierta))
          AND r.marca_baja = false
    LEFT JOIN LATERAL (
        SELECT (
            EXISTS (
                SELECT 1 FROM Ventas v
                WHERE v.usuario_creacion = s.usuario_id
                  AND v.estado = 2
                  AND v.marca_baja = false
                  AND s.hora_sesion_cerrada IS NOT NULL
                  AND v.fecha_creacion BETWEEN s.hora_sesion_abierta AND s.hora_sesion_cerrada
            )
            OR EXISTS (
                SELECT 1 FROM Movimientos m
                WHERE m.usuario_creacion = s.usuario_id
                  AND m.marca_baja = false
                  AND s.hora_sesion_cerrada IS NOT NULL
                  AND m.fecha_creacion BETWEEN s.hora_sesion_abierta AND s.hora_sesion_cerrada
            )
        ) AS tuvo_actividad
    ) act ON true
    WHERE s.marca_baja = false
      AND (p_usuario_id IS NULL OR s.usuario_id = p_usuario_id)
      AND (p_sucursal_id IS NULL OR u.sucursal_id = p_sucursal_id)
      AND (
            p_solo_activas IS NULL
            OR (p_solo_activas = true  AND s.hora_sesion_cerrada IS NULL)
            OR (p_solo_activas = false AND s.hora_sesion_cerrada IS NOT NULL)
          )
      AND (p_fecha_desde IS NULL OR s.hora_sesion_abierta::DATE >= p_fecha_desde)
      AND (p_fecha_hasta IS NULL OR s.hora_sesion_abierta::DATE <= p_fecha_hasta)
      AND (
            p_texto_busqueda IS NULL
            OR u.usuario ILIKE '%' || p_texto_busqueda || '%'
            OR u.nombres ILIKE '%' || p_texto_busqueda || '%'
            OR u.apellidos ILIKE '%' || p_texto_busqueda || '%'
          )
    ORDER BY s.hora_sesion_abierta DESC;
END;
$$;