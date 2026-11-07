DROP FUNCTION IF EXISTS spInventarioKpiResumen();
DROP FUNCTION IF EXISTS spInventarioKpiResumen(INT);

CREATE OR REPLACE FUNCTION spInventarioKpiResumen(
    p_sucursal_id INT DEFAULT NULL
)
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
        (
            SELECT COUNT(DISTINCT p.producto_id)
            FROM productos p
            WHERE p.marca_baja = FALSE
              AND (
                  p_sucursal_id IS NULL
                  OR EXISTS (
                      SELECT 1 FROM inventario inv
                      WHERE inv.producto_id = p.producto_id
                        AND inv.sucursal_id = p_sucursal_id
                        AND inv.marca_baja = FALSE
                  )
              )
        )::BIGINT AS total_productos_catalogo,
        
        (
            SELECT COALESCE(SUM(i.stock), 0)
            FROM inventario i
            WHERE i.marca_baja = FALSE
              AND (p_sucursal_id IS NULL OR i.sucursal_id = p_sucursal_id)
        )::BIGINT AS unidades_stock_activo,
        
        (
            SELECT COUNT(*)
            FROM inventario_pendientes ip
            WHERE ip.marca_baja = FALSE
              AND ip.estado IN (1, 4)
              AND (p_sucursal_id IS NULL OR ip.sucursal_id = p_sucursal_id)
        )::BIGINT AS lotes_sala_espera,
        
        (
            SELECT COUNT(*)
            FROM inventario i
            INNER JOIN lotes l ON i.lote_id = l.lote_id
            WHERE i.marca_baja = FALSE 
              AND l.marca_baja = FALSE 
              AND i.stock > 0 
              AND (p_sucursal_id IS NULL OR i.sucursal_id = p_sucursal_id)
              AND l.fecha_vencimiento IS NOT NULL 
              AND (l.fecha_vencimiento - CURRENT_DATE) <= 7
        )::BIGINT AS alertas_vencimiento;
END;
$$;
