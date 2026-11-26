-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     LISTAR MOVIMIENTOS DE CAJA CON FILTROS DINAMICOS Y ESTADO DE CONSOLIDACION
-- ============================================================================================
DROP FUNCTION IF EXISTS fn_movimientos_listar(INT, INT, BOOLEAN, INT, INT, DATE, DATE);
DROP FUNCTION IF EXISTS fn_movimientos_listar(INT, INT, BOOLEAN, INT, INT, DATE, DATE, VARCHAR);

CREATE OR REPLACE FUNCTION fn_movimientos_listar(
    p_sucursal_id INT DEFAULT NULL,
    p_reporte_id INT DEFAULT NULL,
    p_solo_pendientes BOOLEAN DEFAULT FALSE,
    p_tipo_movimiento INT DEFAULT NULL,
    p_usuario_id INT DEFAULT NULL,
    p_fecha_desde DATE DEFAULT NULL,
    p_fecha_hasta DATE DEFAULT NULL,
    p_estado VARCHAR DEFAULT NULL -- 'PENDIENTES', 'CONSOLIDADOS', NULL/'TODOS'
)
RETURNS TABLE (
    movimiento_id INT,
    reporte_id INT,
    monto DECIMAL(8,2),
    tipo_movimiento INT,
    descripcion_tipo VARCHAR,
    metodo_movimiento INT,
    descripcion_metodo VARCHAR,
    detalle VARCHAR,
    usuario_creacion INT,
    nombre_usuario VARCHAR,
    rol_usuario INT,
    sucursal_id INT,
    fecha_creacion TIMESTAMP,
    requiere_consolidacion BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.movimiento_id,
        m.reporte_id,
        m.monto,
        m.tipo_movimiento,
        ct.descripcion::VARCHAR AS descripcion_tipo,
        m.metodo_movimiento,
        cm.descripcion::VARCHAR AS descripcion_metodo,
        m.detalle::VARCHAR,
        m.usuario_creacion,
        (u.nombres || ' ' || u.apellidos)::VARCHAR AS nombre_usuario,
        u.rol_id AS rol_usuario,
        m.sucursal_id,
        m.fecha_creacion,
        (
            m.reporte_id IS NULL
            AND (
                u.rol_id <> 1
                OR NOT EXISTS (
                    SELECT 1
                    FROM Sesiones s
                    WHERE s.usuario_id = m.usuario_creacion
                      AND s.hora_sesion_cerrada IS NULL
                      AND s.marca_baja = false
                      AND m.fecha_creacion >= s.hora_sesion_abierta
                )
            )
        ) AS requiere_consolidacion
    FROM Movimientos m
    INNER JOIN Usuarios u
            ON m.usuario_creacion = u.usuario_id
    LEFT JOIN Conceptos ct
           ON m.tipo_movimiento = ct.codigo
          AND ct.grupo_id = 6
          AND ct.marca_baja = false
    LEFT JOIN Conceptos cm
           ON m.metodo_movimiento = cm.codigo
          AND cm.grupo_id = 4
          AND cm.marca_baja = false
    WHERE m.marca_baja = false
      AND (p_sucursal_id IS NULL OR m.sucursal_id = p_sucursal_id)
      AND (p_reporte_id IS NULL OR m.reporte_id = p_reporte_id)
      AND (
            (p_estado IS NULL AND (p_solo_pendientes = false OR m.reporte_id IS NULL))
            OR (UPPER(p_estado) = 'PENDIENTES' AND m.reporte_id IS NULL)
            OR (UPPER(p_estado) = 'CONSOLIDADOS' AND m.reporte_id IS NOT NULL)
            OR (UPPER(p_estado) = 'TODOS')
          )
      AND (p_tipo_movimiento IS NULL OR m.tipo_movimiento = p_tipo_movimiento)
      AND (p_usuario_id IS NULL OR m.usuario_creacion = p_usuario_id)
      AND (p_fecha_desde IS NULL OR m.fecha_creacion::DATE >= p_fecha_desde)
      AND (p_fecha_hasta IS NULL OR m.fecha_creacion::DATE <= p_fecha_hasta)
    ORDER BY m.fecha_creacion DESC;
END;
$$;
