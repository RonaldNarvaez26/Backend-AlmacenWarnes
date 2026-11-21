-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     LISTADO DE CLIENTES CON BÚSQUEDA Y TOTAL DE COMPRAS REALIZADAS
-- ============================================================================================
CREATE OR REPLACE FUNCTION sp_clientes_listar(
    p_busqueda VARCHAR DEFAULT NULL,
    p_estado VARCHAR DEFAULT 'ACTIVOS' -- 'ACTIVOS', 'BAJAS', 'TODOS'
)
RETURNS TABLE (
    cliente_id INT,
    nit VARCHAR(20),
    nombres VARCHAR(50),
    apellidos VARCHAR(50),
    nombre_razon_social VARCHAR(50),
    telefono VARCHAR(20),
    marca_baja BOOLEAN,
    fecha_creacion TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    total_compras BIGINT,
    monto_total_comprado DECIMAL(10,2)
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
DECLARE
    v_busqueda_limpia VARCHAR := NULLIF(TRIM(p_busqueda), '');
    v_estado_limpio VARCHAR := UPPER(COALESCE(TRIM(p_estado), 'ACTIVOS'));
BEGIN
    RETURN QUERY
    SELECT 
        c.cliente_id,
        c.nit,
        c.nombres,
        c.apellidos,
        c.nombre_razon_social,
        c.telefono,
        c.marca_baja,
        c.fecha_creacion,
        c.fecha_modificacion,
        COUNT(v.venta_id)::BIGINT AS total_compras,
        COALESCE(SUM(CASE WHEN v.estado = 2 THEN v.precio_total ELSE 0 END), 0)::DECIMAL(10,2) AS monto_total_comprado
    FROM Clientes c
    LEFT JOIN Ventas v ON v.cliente_id = c.cliente_id AND v.marca_baja = false
    WHERE 
        (
            v_estado_limpio = 'TODOS'
            OR (v_estado_limpio = 'ACTIVOS' AND c.marca_baja = false)
            OR (v_estado_limpio = 'BAJAS' AND c.marca_baja = true)
        )
        AND (
            v_busqueda_limpia IS NULL
            OR c.nit ILIKE '%' || v_busqueda_limpia || '%'
            OR c.nombres ILIKE '%' || v_busqueda_limpia || '%'
            OR c.apellidos ILIKE '%' || v_busqueda_limpia || '%'
            OR c.nombre_razon_social ILIKE '%' || v_busqueda_limpia || '%'
            OR c.telefono ILIKE '%' || v_busqueda_limpia || '%'
        )
    GROUP BY 
        c.cliente_id,
        c.nit,
        c.nombres,
        c.apellidos,
        c.nombre_razon_social,
        c.telefono,
        c.marca_baja,
        c.fecha_creacion,
        c.fecha_modificacion
    ORDER BY c.fecha_creacion DESC;
END;
$$;
