-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Retorna la configuración actual de días de anticipación de vencimiento
--                  por cada categoría activa de producto (Grupo 2 vs Grupo 11 de Conceptos)
--                  junto con la cantidad de lotes afectados actualmente.
-- ============================================================================================

DROP FUNCTION IF EXISTS fn_notificaciones_config_vencimiento_listar();

CREATE OR REPLACE FUNCTION fn_notificaciones_config_vencimiento_listar()
RETURNS TABLE (
    categoria_id INT,
    nombre_categoria VARCHAR,
    dias_anticipacion INT,
    lotes_afectados INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        cat.codigo::INT AS categoria_id,
        cat.descripcion::VARCHAR AS nombre_categoria,
        COALESCE(NULLIF(regexp_replace(umb.descripcion, '[^0-9]', '', 'g'), '')::INT, 30) AS dias_anticipacion,
        COUNT(DISTINCT l.lote_id)::INT AS lotes_afectados
    FROM conceptos cat
    LEFT JOIN conceptos umb 
        ON umb.grupo_id = 11 
       AND umb.codigo = cat.codigo 
       AND umb.marca_baja = FALSE
    LEFT JOIN productos p 
        ON p.categoria_id = cat.codigo 
       AND p.marca_baja = FALSE
    LEFT JOIN inventario i 
        ON i.producto_id = p.producto_id 
       AND i.marca_baja = FALSE 
       AND i.stock > 0
    LEFT JOIN lotes l 
        ON l.lote_id = i.lote_id 
       AND l.marca_baja = FALSE 
       AND l.fecha_vencimiento IS NOT NULL
       AND l.fecha_vencimiento <= (CURRENT_DATE + (COALESCE(NULLIF(regexp_replace(umb.descripcion, '[^0-9]', '', 'g'), ''), '30')::INT || ' days')::INTERVAL)
    WHERE cat.grupo_id = 2 
      AND cat.codigo > 0 
      AND cat.marca_baja = FALSE
    GROUP BY cat.codigo, cat.descripcion, umb.descripcion
    ORDER BY cat.codigo ASC;
END;
$$;
