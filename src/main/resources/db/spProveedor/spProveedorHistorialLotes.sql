CREATE OR REPLACE FUNCTION spProveedorHistorialLotes(
    p_proveedor_id INT,
    p_filtro_estado VARCHAR DEFAULT 'todos'
)
RETURNS TABLE (
    lote_id INT,
    descripcion VARCHAR(100),
    producto_id INT,
    nombre_producto VARCHAR(100),
    codigo_interno VARCHAR(10),
    cantidad_inicial INT,
    stock_actual INT,
    costo_unitario NUMERIC(8,2),
    precio_lote NUMERIC(8,2),
    fecha_vencimiento DATE,
    dias_para_vencer INT,
    estado_caducidad VARCHAR(20),
    fecha_ingreso TIMESTAMP
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
DECLARE
    v_filtro VARCHAR := LOWER(COALESCE(TRIM(p_filtro_estado), 'todos'));
BEGIN
    RETURN QUERY
    SELECT
        l.lote_id,
        l.descripcion,
        pr.producto_id,
        pr.nombre_producto,
        pr.codigo_interno,
        l.cantidad AS cantidad_inicial,
        COALESCE(i.stock, 0)::INT AS stock_actual,
        l.costo_unitario,
        l.precio_lote,
        l.fecha_vencimiento,
        CASE 
            WHEN l.fecha_vencimiento IS NULL THEN NULL
            ELSE (l.fecha_vencimiento - CURRENT_DATE)::INT
        END AS dias_para_vencer,
        CASE
            WHEN l.fecha_vencimiento IS NULL THEN 'NO_PERECEDERO'
            WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 THEN 'VENCIDO'
            WHEN (l.fecha_vencimiento - CURRENT_DATE) <= 7 THEN 'CRITICO'
            WHEN (l.fecha_vencimiento - CURRENT_DATE) <= 14 THEN 'PROXIMO'
            ELSE 'VIGENTE'
        END::VARCHAR(20) AS estado_caducidad,
        l.fecha_creacion AS fecha_ingreso
    FROM lotes l
    LEFT JOIN inventario i ON i.lote_id = l.lote_id AND i.marca_baja = FALSE
    LEFT JOIN productos pr ON pr.producto_id = i.producto_id AND pr.marca_baja = FALSE
    WHERE l.proveedor_id = p_proveedor_id
      AND l.marca_baja = FALSE
      AND (
          v_filtro = 'todos'
          OR (v_filtro = 'activos' AND COALESCE(i.stock, 0) > 0)
          OR (v_filtro = 'proximos_vencer' AND COALESCE(i.stock, 0) > 0 AND l.fecha_vencimiento IS NOT NULL AND (l.fecha_vencimiento - CURRENT_DATE) <= 14 AND (l.fecha_vencimiento - CURRENT_DATE) >= 0)
          OR (v_filtro = 'vencidos' AND l.fecha_vencimiento IS NOT NULL AND (l.fecha_vencimiento - CURRENT_DATE) < 0)
      )
    ORDER BY 
        CASE WHEN l.fecha_vencimiento IS NULL THEN 1 ELSE 0 END,
        l.fecha_vencimiento ASC, 
        l.lote_id DESC;
END;
$$;

