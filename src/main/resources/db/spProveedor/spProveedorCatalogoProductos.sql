CREATE OR REPLACE FUNCTION spProveedorCatalogoProductos(p_proveedor_id INT)
RETURNS TABLE (
    producto_id INT,
    codigo_interno VARCHAR(10),
    codigo_barras VARCHAR(100),
    nombre_producto VARCHAR(100),
    categoria_nombre VARCHAR(100),
    precio_compra NUMERIC(8,2),
    precio_venta NUMERIC(8,2),
    estado_producto INT,
    stock_actual BIGINT,
    total_lotes_recibidos BIGINT,
    fecha_ultima_entrega TIMESTAMP
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
BEGIN
    RETURN QUERY
    SELECT
        pr.producto_id,
        pr.codigo_interno,
        pr.codigo_barras,
        pr.nombre_producto,
        COALESCE(c.descripcion, 'General')::VARCHAR(100) AS categoria_nombre,
        pr.precio_compra,
        pr.precio_venta,
        pr.estado AS estado_producto,
        COALESCE(SUM(i.stock), 0)::BIGINT AS stock_actual,
        COUNT(DISTINCT l.lote_id)::BIGINT AS total_lotes_recibidos,
        MAX(l.fecha_creacion) AS fecha_ultima_entrega
    FROM lotes l
    JOIN inventario i ON i.lote_id = l.lote_id AND i.marca_baja = FALSE
    JOIN productos pr ON pr.producto_id = i.producto_id AND pr.marca_baja = FALSE
    LEFT JOIN conceptos c ON c.codigo = pr.categoria_id AND c.grupo_id = 2 AND c.marca_baja = FALSE
    WHERE l.proveedor_id = p_proveedor_id
      AND l.marca_baja = FALSE
    GROUP BY pr.producto_id, pr.codigo_interno, pr.codigo_barras, pr.nombre_producto, c.descripcion, pr.precio_compra, pr.precio_venta, pr.estado
    ORDER BY pr.nombre_producto ASC;
END;
$$;

