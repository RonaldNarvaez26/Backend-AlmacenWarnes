CREATE OR REPLACE PROCEDURE spInventarioIngresoDirectoSupervisor(
    p_producto_id INT, 
    p_categoria_id INT, 
    p_nombre_producto VARCHAR(100),
    p_codigo_barras VARCHAR(100), 
    p_codigo_interno VARCHAR(10), 
    p_proveedor_id INT,
    p_descripcion_lote VARCHAR(100), 
    p_stock_ingresado INT, 
    p_precio_lote DECIMAL(8,2),
    p_precio_compra DECIMAL(8,2), 
    p_precio_venta DECIMAL(8,2), 
    p_fecha_vencimiento DATE,
    p_usuario_creacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nuevo_lote_id INT;
    v_sucursal_fija INT := 1;
BEGIN
    -- Si no viene ID de producto pero viene codigo de barras no vacio, buscar si ya existe
    IF p_producto_id IS NULL AND p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' THEN
        SELECT producto_id INTO p_producto_id 
        FROM productos 
        WHERE codigo_barras = TRIM(p_codigo_barras) AND marca_baja = FALSE 
        LIMIT 1;
    END IF;

    IF p_producto_id IS NULL THEN
        INSERT INTO productos 
            (categoria_id, codigo_barras, nombre_producto, codigo_interno, precio_compra, precio_venta, estado, fecha_creacion, usuario_creacion, marca_baja)
        VALUES 
            (p_categoria_id, NULLIF(TRIM(p_codigo_barras), ''), p_nombre_producto, p_codigo_interno, p_precio_compra, p_precio_venta, 1, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE)
        RETURNING producto_id INTO p_producto_id;
    ELSE
        IF p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' AND EXISTS (
            SELECT 1 FROM productos 
            WHERE codigo_barras = TRIM(p_codigo_barras) AND producto_id <> p_producto_id AND marca_baja = FALSE
        ) THEN
            RAISE EXCEPTION 'El codigo de barras ya pertenece a otro producto distinto.';
        END IF;

        UPDATE productos
        SET nombre_producto = p_nombre_producto, 
            categoria_id = p_categoria_id, 
            codigo_barras = NULLIF(TRIM(p_codigo_barras), ''),
            precio_compra = p_precio_compra, 
            precio_venta = p_precio_venta, 
            fecha_modificacion = CURRENT_TIMESTAMP, 
            usuario_modificacion = p_usuario_creacion
        WHERE producto_id = p_producto_id AND marca_baja = FALSE;
    END IF;

    INSERT INTO lotes
        (proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, fecha_vencimiento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (p_proveedor_id, COALESCE(p_descripcion_lote, 'Lote ' || p_codigo_interno), p_stock_ingresado, COALESCE(p_precio_lote, p_precio_compra * p_stock_ingresado), p_precio_compra, p_fecha_vencimiento, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE)
    RETURNING lote_id INTO v_nuevo_lote_id;

    INSERT INTO inventario
        (sucursal_id, lote_id, producto_id, stock, en_descuento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (v_sucursal_fija, v_nuevo_lote_id, p_producto_id, p_stock_ingresado, FALSE, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE);
END;
$$;
