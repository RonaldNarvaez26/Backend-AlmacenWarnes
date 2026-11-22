DROP FUNCTION IF EXISTS spInventarioLotesListar();
DROP FUNCTION IF EXISTS spInventarioLotesListar(VARCHAR);
DROP FUNCTION IF EXISTS spInventarioLotesListar(VARCHAR, INT);

CREATE OR REPLACE FUNCTION spInventarioLotesListar(
    p_estado VARCHAR DEFAULT 'ACTIVOS',
    p_sucursal_id INT DEFAULT NULL
)
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
    proveedor_id INT,
    nombre_proveedor VARCHAR,
    dias_para_vencer INT,
    marca_baja BOOLEAN,
    sucursal_id INT,
    codigo_sucursal VARCHAR,
    ciudad_sucursal VARCHAR
) AS $$
#variable_conflict use_column
DECLARE
    v_estado VARCHAR := UPPER(COALESCE(TRIM(p_estado), 'ACTIVOS'));
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
        l.proveedor_id,
        COALESCE(pr.nombre_empresa, 'Compra Directa')::VARCHAR AS nombre_proveedor,
        CASE
            WHEN l.fecha_vencimiento IS NOT NULL THEN (l.fecha_vencimiento - CURRENT_DATE)::INT
            ELSE NULL
        END AS dias_para_vencer,
        i.marca_baja,
        i.sucursal_id,
        s.codigo_interno::VARCHAR AS codigo_sucursal,
        s.ciudad::VARCHAR AS ciudad_sucursal
    FROM inventario i
    INNER JOIN productos p ON i.producto_id = p.producto_id
    INNER JOIN lotes l ON i.lote_id = l.lote_id
    INNER JOIN sucursales s ON i.sucursal_id = s.sucursal_id
    LEFT JOIN proveedores pr ON l.proveedor_id = pr.proveedor_id
    WHERE (p_sucursal_id IS NULL OR i.sucursal_id = p_sucursal_id)
      AND (
          v_estado = 'TODOS'
          OR (v_estado = 'ACTIVOS' AND i.marca_baja = FALSE AND l.marca_baja = FALSE)
          OR (v_estado IN ('INACTIVOS', 'BAJA', 'DADOS_DE_BAJA') AND (i.marca_baja = TRUE OR l.marca_baja = TRUE))
      )
    ORDER BY l.fecha_vencimiento ASC NULLS LAST, i.inventario_id DESC;
END;
$$ LANGUAGE plpgsql;
