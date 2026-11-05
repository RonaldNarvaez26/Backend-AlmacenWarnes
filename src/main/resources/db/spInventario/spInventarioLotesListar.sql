CREATE OR REPLACE FUNCTION spInventarioLotesListar()
RETURNS TABLE (
    inventario_id INT,
    stock INT,
    en_descuento BOOLEAN,
    precio_descuento DECIMAL(8,2),
    producto_id INT,
    codigo_interno VARCHAR,
    codigo_barras VARCHAR,
    nombre_producto VARCHAR,
    precio_venta DECIMAL(8,2),
    estado_producto INT,
    lote_id INT,
    descripcion_lote VARCHAR,
    costo_unitario DECIMAL(8,2),
    fecha_vencimiento DATE,
    nombre_proveedor VARCHAR,
    dias_para_vencer INT
) AS $$
#variable_conflict use_column
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
        COALESCE(pr.nombre_empresa, 'Compra Directa')::VARCHAR AS nombre_proveedor,
        CASE
            WHEN l.fecha_vencimiento IS NOT NULL THEN (l.fecha_vencimiento - CURRENT_DATE)::INT
            ELSE NULL
        END AS dias_para_vencer
    FROM inventario i
    INNER JOIN productos p ON i.producto_id = p.producto_id
    INNER JOIN lotes l ON i.lote_id = l.lote_id
    LEFT JOIN proveedores pr ON l.proveedor_id = pr.proveedor_id
    WHERE i.marca_baja = FALSE
      AND i.sucursal_id = 1
      AND p.marca_baja = FALSE
      AND l.marca_baja = FALSE
    ORDER BY l.fecha_vencimiento ASC NULLS LAST, i.inventario_id DESC;
END;
$$ LANGUAGE plpgsql;
