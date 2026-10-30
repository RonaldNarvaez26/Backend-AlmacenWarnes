CREATE OR REPLACE FUNCTION spInventarioLotesListar()
RETURNS TABLE (
    inventario_id INT, 
    stock INT, 
    en_descuento BOOLEAN, 
    precio_descuento DECIMAL,
    producto_id INT, 
    codigo_interno VARCHAR, 
    codigo_barras VARCHAR, 
    nombre_producto VARCHAR,
    precio_venta DECIMAL, 
    estado_producto INT, 
    lote_id INT, 
    descripcion_lote VARCHAR,
    costo_unitario DECIMAL, 
    fecha_vencimiento DATE, 
    nombre_proveedor VARCHAR, 
    dias_para_vencer INT
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        i.inventario_id, 
        i.stock, 
        i.en_descuento, 
        i.precio_descuento,
        p.producto_id, 
        p.codigo_interno, 
        p.codigo_barras, 
        p.nombre_producto,
        p.precio_venta, 
        p.estado AS estado_producto,
        l.lote_id, 
        l.descripcion AS descripcion_lote, 
        l.costo_unitario, 
        l.fecha_vencimiento,
        COALESCE(prov.nombre_empresa, 'Compra Directa')::VARCHAR AS nombre_proveedor,
        CASE 
            WHEN l.fecha_vencimiento IS NULL THEN NULL
            ELSE (l.fecha_vencimiento - CURRENT_DATE)::INT
        END AS dias_para_vencer
    FROM inventario i
    INNER JOIN productos p ON i.producto_id = p.producto_id
    INNER JOIN lotes l ON i.lote_id = l.lote_id
    LEFT JOIN proveedores prov ON l.proveedor_id = prov.proveedor_id
    WHERE i.marca_baja = FALSE 
      AND p.marca_baja = FALSE 
      AND l.marca_baja = FALSE 
      AND i.sucursal_id = 1
    ORDER BY 
        CASE WHEN l.fecha_vencimiento IS NULL THEN 1 ELSE 0 END, 
        (l.fecha_vencimiento - CURRENT_DATE) ASC;
END;
$$;