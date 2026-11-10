CREATE OR REPLACE FUNCTION fn_historial_inventario_listar(
    p_sucursal_id INT DEFAULT NULL,
    p_producto_id INT DEFAULT NULL,
    p_tipo_movimiento INT DEFAULT NULL
)
RETURNS TABLE (
    historial_id INT,
    sucursal_id INT,
    codigo_sucursal VARCHAR,
    ciudad_sucursal VARCHAR,
    producto_id INT,
    codigo_interno VARCHAR,
    codigo_barras VARCHAR,
    nombre_producto VARCHAR,
    lote_id INT,
    descripcion_lote VARCHAR,
    tipo_movimiento INT,
    nombre_tipo_movimiento VARCHAR,
    stock_anterior INT,
    cantidad INT,
    stock_resultante INT,
    descripcion VARCHAR,
    marca_baja BOOLEAN,
    fecha_creacion TIMESTAMP,
    usuario_creacion INT,
    nombre_usuario_creacion VARCHAR,
    usuario_autorizacion INT,
    nombre_usuario_autorizacion VARCHAR
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
BEGIN
    RETURN QUERY
    SELECT 
        h.historial_id,
        h.sucursal_id,
        s.codigo_interno AS codigo_sucursal,
        s.ciudad AS ciudad_sucursal,
        h.producto_id,
        p.codigo_interno,
        p.codigo_barras,
        p.nombre_producto,
        NULL::INT AS lote_id,
        NULL::VARCHAR AS descripcion_lote,
        h.tipo_movimiento,
        COALESCE(c.descripcion, 'Movimiento ' || h.tipo_movimiento)::VARCHAR AS nombre_tipo_movimiento,
        h.stock_anterior,
        ABS(h.stock_resultante - h.stock_anterior)::INT AS cantidad,
        h.stock_resultante,
        h.descripcion,
        h.marca_baja,
        h.fecha_creacion,
        h.usuario_creacion,
        (COALESCE(uc.nombres, '') || ' ' || COALESCE(uc.apellidos, ''))::VARCHAR AS nombre_usuario_creacion,
        h.usuario_autorizacion,
        (COALESCE(ua.nombres, '') || ' ' || COALESCE(ua.apellidos, ''))::VARCHAR AS nombre_usuario_autorizacion
    FROM historial_inventario h
    JOIN productos p ON p.producto_id = h.producto_id
    JOIN sucursales s ON s.sucursal_id = h.sucursal_id
    LEFT JOIN conceptos c ON c.grupo_id = 9 AND c.codigo = h.tipo_movimiento AND c.marca_baja = FALSE
    LEFT JOIN usuarios uc ON uc.usuario_id = h.usuario_creacion
    LEFT JOIN usuarios ua ON ua.usuario_id = h.usuario_autorizacion
    WHERE (p_sucursal_id IS NULL OR p_sucursal_id = 0 OR h.sucursal_id = p_sucursal_id)
      AND (p_producto_id IS NULL OR p_producto_id = 0 OR h.producto_id = p_producto_id)
      AND (p_tipo_movimiento IS NULL OR p_tipo_movimiento = 0 OR h.tipo_movimiento = p_tipo_movimiento)
      AND h.marca_baja = FALSE
    ORDER BY h.fecha_creacion DESC, h.historial_id DESC;
END;
$$;
