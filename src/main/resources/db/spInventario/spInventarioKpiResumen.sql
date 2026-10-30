CREATE OR REPLACE FUNCTION spInventarioKpiResumen()
RETURNS TABLE (
    total_productos_catalogo BIGINT, 
    unidades_stock_activo BIGINT, 
    lotes_sala_espera BIGINT, 
    alertas_vencimiento BIGINT
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        (SELECT COUNT(*) FROM productos WHERE marca_baja = FALSE),
        (SELECT COALESCE(SUM(stock), 0) FROM inventario WHERE sucursal_id = 1 AND marca_baja = FALSE),
        (SELECT COUNT(*) FROM inventario_pendientes WHERE sucursal_id = 1 AND estado = 1 AND marca_baja = FALSE),
        (SELECT COUNT(*) FROM inventario i
         INNER JOIN lotes l ON i.lote_id = l.lote_id
         WHERE i.sucursal_id = 1 
           AND i.marca_baja = FALSE 
           AND l.marca_baja = FALSE 
           AND i.stock > 0 
           AND l.fecha_vencimiento IS NOT NULL 
           AND (l.fecha_vencimiento - CURRENT_DATE) <= 7);
END;
$$;