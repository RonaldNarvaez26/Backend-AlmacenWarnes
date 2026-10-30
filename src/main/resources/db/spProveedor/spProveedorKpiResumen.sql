CREATE OR REPLACE FUNCTION spProveedorKpiResumen()
RETURNS TABLE (
    total_proveedores_activos BIGINT,
    proveedores_alertas_vencimiento BIGINT,
    total_productos_abastecidos BIGINT,
    total_lotes_activos BIGINT,
    valor_total_inventario NUMERIC(12,2)
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        -- 1. Total proveedores activos en sistema
        (SELECT COUNT(*) FROM proveedores WHERE marca_baja = FALSE)::BIGINT AS total_proveedores_activos,

        -- 2. Proveedores con lotes proximos a vencer (caducidad <= 7 dias y stock > 0)
        (SELECT COUNT(DISTINCT l.proveedor_id)
         FROM lotes l
         JOIN inventario i ON i.lote_id = l.lote_id
         JOIN proveedores p ON p.proveedor_id = l.proveedor_id
         WHERE l.marca_baja = FALSE
           AND i.marca_baja = FALSE
           AND p.marca_baja = FALSE
           AND i.stock > 0
           AND l.fecha_vencimiento IS NOT NULL
           AND (l.fecha_vencimiento - CURRENT_DATE) <= 7)::BIGINT AS proveedores_alertas_vencimiento,

        -- 3. Total productos distintos abastecidos por proveedores activos
        (SELECT COUNT(DISTINCT i.producto_id)
         FROM inventario i
         JOIN lotes l ON i.lote_id = l.lote_id
         JOIN proveedores p ON p.proveedor_id = l.proveedor_id
         WHERE i.marca_baja = FALSE
           AND l.marca_baja = FALSE
           AND p.marca_baja = FALSE)::BIGINT AS total_productos_abastecidos,

        -- 4. Total de lotes activos con stock > 0 de proveedores activos
        (SELECT COUNT(DISTINCT l.lote_id)
         FROM lotes l
         JOIN inventario i ON i.lote_id = l.lote_id
         JOIN proveedores p ON p.proveedor_id = l.proveedor_id
         WHERE l.marca_baja = FALSE
           AND i.marca_baja = FALSE
           AND p.marca_baja = FALSE
           AND i.stock > 0)::BIGINT AS total_lotes_activos,

        -- 5. Valor total de mercaderia activa abastecida (stock * costo_unitario)
        (SELECT COALESCE(SUM(i.stock * l.costo_unitario), 0)::NUMERIC(12,2)
         FROM inventario i
         JOIN lotes l ON i.lote_id = l.lote_id
         JOIN proveedores p ON p.proveedor_id = l.proveedor_id
         WHERE i.marca_baja = FALSE
           AND l.marca_baja = FALSE
           AND p.marca_baja = FALSE
           AND i.stock > 0)::NUMERIC(12,2) AS valor_total_inventario;
END;
$$;

