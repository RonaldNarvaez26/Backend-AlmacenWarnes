CREATE OR REPLACE FUNCTION spProveedorListar(
    p_busqueda VARCHAR DEFAULT NULL,
    p_producto VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    proveedor_id INT,
    nombre_empresa VARCHAR(50),
    nombre_contacto VARCHAR(50),
    numero_contacto VARCHAR(15),
    anotaciones VARCHAR(200),
    marca_baja BOOLEAN,
    fecha_creacion TIMESTAMP,
    total_productos BIGINT,
    total_lotes BIGINT,
    lotes_activos BIGINT,
    lotes_en_riesgo BIGINT,
    stock_total_acumulado BIGINT,
    valor_mercaderia NUMERIC(12,2)
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
DECLARE
    v_busqueda_limpia VARCHAR := NULLIF(TRIM(p_busqueda), '');
    v_producto_limpio VARCHAR := NULLIF(TRIM(p_producto), '');
BEGIN
    RETURN QUERY
    SELECT
        p.proveedor_id,
        p.nombre_empresa,
        p.nombre_contacto,
        p.numero_contacto,
        p.anotaciones,
        p.marca_baja,
        p.fecha_creacion,
        COUNT(DISTINCT i.producto_id)::BIGINT AS total_productos,
        COUNT(DISTINCT l.lote_id)::BIGINT AS total_lotes,
        COUNT(DISTINCT CASE WHEN i.stock > 0 THEN l.lote_id END)::BIGINT AS lotes_activos,
        COUNT(DISTINCT CASE 
            WHEN i.stock > 0 AND l.fecha_vencimiento IS NOT NULL AND (l.fecha_vencimiento - CURRENT_DATE) <= 7 
            THEN l.lote_id 
        END)::BIGINT AS lotes_en_riesgo,
        COALESCE(SUM(i.stock), 0)::BIGINT AS stock_total_acumulado,
        COALESCE(SUM(i.stock * l.costo_unitario), 0)::NUMERIC(12,2) AS valor_mercaderia
    FROM proveedores p
    LEFT JOIN lotes l ON l.proveedor_id = p.proveedor_id AND l.marca_baja = FALSE
    LEFT JOIN inventario i ON i.lote_id = l.lote_id AND i.marca_baja = FALSE
    WHERE p.marca_baja = FALSE
      -- Filtro por Empresa / Contacto / Contrato
      AND (
          v_busqueda_limpia IS NULL
          OR p.nombre_empresa ILIKE '%' || v_busqueda_limpia || '%'
          OR p.nombre_contacto ILIKE '%' || v_busqueda_limpia || '%'
          OR p.numero_contacto ILIKE '%' || v_busqueda_limpia || '%'
          OR p.anotaciones ILIKE '%' || v_busqueda_limpia || '%'
      )
      -- Filtro cruzado por Producto suministrado
      AND (
          v_producto_limpio IS NULL
          OR EXISTS (
              SELECT 1
              FROM lotes l2
              JOIN inventario i2 ON i2.lote_id = l2.lote_id AND i2.marca_baja = FALSE
              JOIN productos pr2 ON pr2.producto_id = i2.producto_id AND pr2.marca_baja = FALSE
              WHERE l2.proveedor_id = p.proveedor_id
                AND l2.marca_baja = FALSE
                AND (
                    pr2.nombre_producto ILIKE '%' || v_producto_limpio || '%'
                    OR pr2.codigo_interno ILIKE '%' || v_producto_limpio || '%'
                    OR pr2.codigo_barras ILIKE '%' || v_producto_limpio || '%'
                )
          )
      )
    GROUP BY p.proveedor_id, p.nombre_empresa, p.nombre_contacto, p.numero_contacto, p.anotaciones, p.marca_baja, p.fecha_creacion
    ORDER BY p.nombre_empresa ASC;
END;
$$;

