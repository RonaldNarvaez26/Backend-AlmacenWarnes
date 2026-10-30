CREATE OR REPLACE FUNCTION spProductosListarCatalogo()
RETURNS TABLE (
    producto_id INT, 
    codigo_interno VARCHAR, 
    codigo_barras VARCHAR, 
    nombre_producto VARCHAR,
    categoria_id INT, 
    precio_compra DECIMAL, 
    precio_venta DECIMAL, 
    estado INT,
    stock_total_acumulado BIGINT
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.producto_id, 
        p.codigo_interno, 
        p.codigo_barras, 
        p.nombre_producto,
        p.categoria_id, 
        p.precio_compra, 
        p.precio_venta, 
        p.estado,
        COALESCE(SUM(i.stock), 0)::BIGINT AS stock_total_acumulado
    FROM productos p
    LEFT JOIN inventario i 
        ON p.producto_id = i.producto_id 
       AND i.sucursal_id = 1 
       AND i.marca_baja = FALSE
    WHERE p.marca_baja = FALSE
    GROUP BY 
        p.producto_id, 
        p.codigo_interno, 
        p.codigo_barras, 
        p.nombre_producto,
        p.categoria_id, 
        p.precio_compra, 
        p.precio_venta, 
        p.estado
    ORDER BY p.nombre_producto ASC;
END;
$$;