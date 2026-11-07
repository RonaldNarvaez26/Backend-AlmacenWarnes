DROP FUNCTION IF EXISTS spProductosListarCatalogo();
DROP FUNCTION IF EXISTS spProductosListarCatalogo(VARCHAR);
DROP FUNCTION IF EXISTS spProductosListarCatalogo(VARCHAR, INT);

CREATE OR REPLACE FUNCTION spProductosListarCatalogo(
    p_estado VARCHAR DEFAULT 'ACTIVOS',
    p_sucursal_id INT DEFAULT NULL
)
RETURNS TABLE (
    producto_id INT,
    codigo_interno VARCHAR,
    codigo_barras VARCHAR,
    nombre_producto VARCHAR,
    categoria_id INT,
    categoria_nombre VARCHAR,
    precio_compra DECIMAL(8,2),
    precio_venta DECIMAL(8,2),
    estado INT,
    stock_total_acumulado BIGINT,
    marca_baja BOOLEAN
) AS $$
#variable_conflict use_column
DECLARE
    v_estado VARCHAR := UPPER(COALESCE(TRIM(p_estado), 'ACTIVOS'));
BEGIN
    RETURN QUERY
    SELECT
        p.producto_id,
        p.codigo_interno,
        p.codigo_barras,
        p.nombre_producto,
        p.categoria_id,
        COALESCE(c.descripcion, 'General')::VARCHAR AS categoria_nombre,
        p.precio_compra,
        p.precio_venta,
        p.estado,
        COALESCE(SUM(CASE WHEN i.marca_baja = FALSE THEN i.stock ELSE 0 END), 0)::BIGINT AS stock_total_acumulado,
        p.marca_baja
    FROM productos p
    LEFT JOIN conceptos c 
        ON c.grupo_id = 2 
       AND c.codigo = p.categoria_id 
       AND c.marca_baja = FALSE
    LEFT JOIN inventario i 
        ON p.producto_id = i.producto_id 
       AND (p_sucursal_id IS NULL OR i.sucursal_id = p_sucursal_id)
    WHERE (
        v_estado = 'TODOS'
        OR (v_estado = 'ACTIVOS' AND p.marca_baja = FALSE)
        OR (v_estado IN ('INACTIVOS', 'BAJA', 'DADOS_DE_BAJA') AND p.marca_baja = TRUE)
    )
    AND (
        p_sucursal_id IS NULL
        OR EXISTS (
            SELECT 1 FROM inventario inv 
            WHERE inv.producto_id = p.producto_id 
              AND inv.sucursal_id = p_sucursal_id
        )
    )
    GROUP BY 
        p.producto_id, 
        p.codigo_interno, 
        p.codigo_barras, 
        p.nombre_producto, 
        p.categoria_id, 
        c.descripcion,
        p.precio_compra, 
        p.precio_venta, 
        p.estado,
        p.marca_baja
    ORDER BY p.nombre_producto ASC;
END;
$$ LANGUAGE plpgsql;
