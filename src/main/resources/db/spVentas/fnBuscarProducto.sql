-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     BUSCADOR ÁGIL PARA TERMINAL POS (CON O SIN STOCK)
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_pos_buscar_producto(
    p_sucursal_id INT,
    p_criterio VARCHAR -- Puede ser código de barras, código interno o parte del nombre
)
RETURNS TABLE (
    inventario_id INT,
    producto_id INT,
    codigo_barras VARCHAR,
    codigo_interno VARCHAR,
    nombre_producto VARCHAR,
    precio_venta DECIMAL(8,2),
    stock_disponible INT,
    en_descuento BOOLEAN,
    precio_descuento DECIMAL(8,2)
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        i.inventario_id,
        p.producto_id,
        p.codigo_barras,
        p.codigo_interno,
        p.nombre_producto,
        p.precio_venta,
        i.stock AS stock_disponible,
        i.en_descuento,
        i.precio_descuento
    FROM Inventario i
    INNER JOIN Productos p ON i.producto_id = p.producto_id
    WHERE i.sucursal_id = p_sucursal_id
      AND i.marca_baja = false
      AND p.marca_baja = false
      AND p.estado = 1 -- Solo productos Activos
      AND (
          p.codigo_barras = p_criterio 
          OR p.codigo_interno = p_criterio 
          OR p.nombre_producto ILIKE '%' || p_criterio || '%'
      )
    ORDER BY 
        i.stock DESC, -- Prioriza mostrar los lotes que sí tienen stock primero
        i.fecha_creacion ASC; -- PEPS (Primeros en entrar, primeros en salir)
END;
$$;