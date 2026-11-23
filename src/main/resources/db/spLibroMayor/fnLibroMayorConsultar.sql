-- ============================================================================================
-- AUTOR:           Antigravity / Almacen Warnes
-- DESCRIPCION:     CONSULTA DE MOVIMIENTOS Y BAJAS DEL LIBRO MAYOR CON FILTROS AVANZADOS
--                  (SUCURSAL, FECHAS, HORAS, CAJERO, PRODUCTO, TIPO, METODO, BAJAS)
-- ============================================================================================
DROP FUNCTION IF EXISTS fn_libro_mayor_consultar(INT, DATE, DATE, BOOLEAN, INT, INT);
DROP FUNCTION IF EXISTS fn_libro_mayor_consultar(INT, DATE, DATE, TIME, TIME, INT, INT, INT, INT, BOOLEAN);

CREATE OR REPLACE FUNCTION fn_libro_mayor_consultar(
    p_sucursal_id INT DEFAULT NULL,
    p_fecha_inicio DATE DEFAULT CURRENT_DATE,
    p_fecha_fin DATE DEFAULT CURRENT_DATE,
    p_hora_inicio TIME DEFAULT NULL,
    p_hora_fin TIME DEFAULT NULL,
    p_cajero_id INT DEFAULT NULL,
    p_producto_id INT DEFAULT NULL,
    p_tipo_movimiento INT DEFAULT NULL,
    p_metodo_movimiento INT DEFAULT NULL,
    p_incluir_bajas BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
    referencia_id VARCHAR,
    fecha_hora TIMESTAMP,
    origen VARCHAR,
    tipo_movimiento INT,
    tipo VARCHAR,
    metodo_movimiento INT,
    metodo VARCHAR,
    categoria_id INT,
    categoria VARCHAR,
    detalle VARCHAR,
    monto NUMERIC(12,2),
    marca_baja BOOLEAN,
    sucursal_id INT,
    sucursal_nombre VARCHAR,
    cajero_id INT,
    cajero_nombre VARCHAR,
    productos_resumen VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    WITH movimientos_totales AS (
        -- 1. VENTAS COMERCIALES (ORIGEN: VENTA)
        SELECT
            ('VNT-' || LPAD(v.venta_id::text, 4, '0'))::VARCHAR AS referencia_id,
            v.fecha_creacion AS fecha_hora,
            'VENTA'::VARCHAR AS origen,
            1 AS tipo_movimiento, -- 1 = ENTRADA
            'ENTRADA'::VARCHAR AS tipo,
            v.metodo_venta AS metodo_movimiento,
            COALESCE(UPPER(cm.descripcion), 'EFECTIVO')::VARCHAR AS metodo,
            1 AS categoria_id, -- 1 = Venta Comercial
            'Venta Comercial'::VARCHAR AS categoria,
            ('Venta #' || v.venta_id || ' - ' || COALESCE(cl.nombre_razon_social, 'Sin Cliente'))::VARCHAR AS detalle,
            v.precio_total::NUMERIC(12,2) AS monto,
            (v.marca_baja OR v.estado = 3) AS marca_baja,
            v.sucursal_id AS sucursal_id,
            COALESCE(s.codigo_interno || ' (' || s.ciudad || ')', 'Sucursal #' || v.sucursal_id)::VARCHAR AS sucursal_nombre,
            v.usuario_creacion AS cajero_id,
            COALESCE(u.nombres || ' ' || u.apellidos, u.usuario, 'Usuario #' || v.usuario_creacion)::VARCHAR AS cajero_nombre,
            COALESCE(
                (
                    SELECT string_agg(p.nombre_producto || ' (x' || dv.cantidad::int || ')', ', ')
                    FROM Detalles_Ventas dv
                    JOIN Productos p ON dv.producto_id = p.producto_id
                    WHERE dv.venta_id = v.venta_id
                ),
                'Sin detalle de productos'
            )::VARCHAR AS productos_resumen
        FROM Ventas v
        LEFT JOIN Sucursales s ON v.sucursal_id = s.sucursal_id
        LEFT JOIN Usuarios u ON v.usuario_creacion = u.usuario_id
        LEFT JOIN Clientes cl ON v.cliente_id = cl.cliente_id
        LEFT JOIN Conceptos cm ON v.metodo_venta = cm.codigo AND cm.grupo_id = 4 AND cm.marca_baja = false
        WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR v.sucursal_id = p_sucursal_id)
          AND (p_fecha_inicio IS NULL OR v.fecha_creacion::DATE >= p_fecha_inicio)
          AND (p_fecha_fin IS NULL OR v.fecha_creacion::DATE <= p_fecha_fin)
          AND (p_hora_inicio IS NULL OR v.fecha_creacion::TIME >= p_hora_inicio)
          AND (p_hora_fin IS NULL OR v.fecha_creacion::TIME <= p_hora_fin)
          AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR v.usuario_creacion = p_cajero_id)
          AND (p_producto_id IS NULL OR p_producto_id <= 0 OR EXISTS (
              SELECT 1 FROM Detalles_Ventas dv2
              WHERE dv2.venta_id = v.venta_id AND dv2.producto_id = p_producto_id
          ))
          AND (p_incluir_bajas = TRUE OR (v.marca_baja = FALSE AND v.estado <> 3))
          AND (p_tipo_movimiento IS NULL OR p_tipo_movimiento <= 0 OR p_tipo_movimiento = 1)
          AND (p_metodo_movimiento IS NULL OR p_metodo_movimiento <= 0 OR v.metodo_venta = p_metodo_movimiento)

        UNION ALL

        -- 2. MOVIMIENTOS EXTRA-VENTA (ORIGEN: EXTRA_VENTA)
        SELECT
            ('MOV-' || LPAD(m.movimiento_id::text, 4, '0'))::VARCHAR AS referencia_id,
            m.fecha_creacion AS fecha_hora,
            'EXTRA_VENTA'::VARCHAR AS origen,
            m.tipo_movimiento AS tipo_movimiento,
            CASE WHEN m.tipo_movimiento = 1 THEN 'ENTRADA' ELSE 'SALIDA' END::VARCHAR AS tipo,
            m.metodo_movimiento AS metodo_movimiento,
            COALESCE(UPPER(cm.descripcion), 'EFECTIVO')::VARCHAR AS metodo,
            m.tipo_movimiento AS categoria_id,
            CASE 
                WHEN ct.descripcion IS NOT NULL THEN ct.descripcion
                WHEN m.tipo_movimiento = 1 THEN 'Ingreso Extraordinario'
                ELSE 'Gasto Operativo'
            END::VARCHAR AS categoria,
            m.detalle::VARCHAR AS detalle,
            m.monto::NUMERIC(12,2) AS monto,
            m.marca_baja AS marca_baja,
            COALESCE(m.sucursal_id, u.sucursal_id, 1) AS sucursal_id,
            COALESCE(s.codigo_interno || ' (' || s.ciudad || ')', 'Sucursal')::VARCHAR AS sucursal_nombre,
            m.usuario_creacion AS cajero_id,
            COALESCE(u.nombres || ' ' || u.apellidos, u.usuario, 'Usuario #' || m.usuario_creacion)::VARCHAR AS cajero_nombre,
            'Movimiento Extra-Venta'::VARCHAR AS productos_resumen
        FROM Movimientos m
        LEFT JOIN Usuarios u ON m.usuario_creacion = u.usuario_id
        LEFT JOIN Sucursales s ON COALESCE(m.sucursal_id, u.sucursal_id) = s.sucursal_id
        LEFT JOIN Conceptos cm ON m.metodo_movimiento = cm.codigo AND cm.grupo_id = 4 AND cm.marca_baja = false
        LEFT JOIN Conceptos ct ON m.tipo_movimiento = ct.codigo AND ct.grupo_id = 6 AND ct.marca_baja = false
        WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR COALESCE(m.sucursal_id, u.sucursal_id) = p_sucursal_id)
          AND (p_fecha_inicio IS NULL OR m.fecha_creacion::DATE >= p_fecha_inicio)
          AND (p_fecha_fin IS NULL OR m.fecha_creacion::DATE <= p_fecha_fin)
          AND (p_hora_inicio IS NULL OR m.fecha_creacion::TIME >= p_hora_inicio)
          AND (p_hora_fin IS NULL OR m.fecha_creacion::TIME <= p_hora_fin)
          AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR m.usuario_creacion = p_cajero_id)
          AND (p_producto_id IS NULL OR p_producto_id <= 0) -- Si se filtra por producto específico, los extra-venta se excluyen
          AND (p_incluir_bajas = TRUE OR m.marca_baja = FALSE)
          AND (p_tipo_movimiento IS NULL OR p_tipo_movimiento <= 0 OR m.tipo_movimiento = p_tipo_movimiento)
          AND (p_metodo_movimiento IS NULL OR p_metodo_movimiento <= 0 OR m.metodo_movimiento = p_metodo_movimiento)
    )
    SELECT * FROM movimientos_totales
    ORDER BY fecha_hora DESC, referencia_id DESC;
END;
$$;
