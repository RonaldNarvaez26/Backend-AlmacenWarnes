CREATE OR REPLACE PROCEDURE spInventarioPendientesAprobar(
    p_inventario_pendiente_id INT,
    p_usuario_revision INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_producto_id INT; 
    v_categoria_id INT; 
    v_nombre_producto VARCHAR(100); 
    v_codigo_barras VARCHAR(100); 
    v_codigo_interno VARCHAR(10);
    v_stock_ingresado INT; 
    v_precio_compra DECIMAL(8,2); 
    v_precio_venta DECIMAL(8,2);
    v_proveedor_id INT; 
    v_descripcion_lote VARCHAR(100); 
    v_precio_lote DECIMAL(8,2); 
    v_fecha_vencimiento DATE; 
    v_sucursal_id INT := 1;
    v_nuevo_lote_id INT;
BEGIN
    SELECT producto_id, categoria_id, nombre_producto, codigo_barras, codigo_interno, 
           stock_ingresado, precio_compra, precio_venta, proveedor_id, descripcion_lote, 
           precio_lote, fecha_vencimiento
    INTO v_producto_id, v_categoria_id, v_nombre_producto, v_codigo_barras, v_codigo_interno,
         v_stock_ingresado, v_precio_compra, v_precio_venta, v_proveedor_id, v_descripcion_lote,
         v_precio_lote, v_fecha_vencimiento
    FROM inventario_pendientes
    WHERE inventario_pendiente_id = p_inventario_pendiente_id 
      AND estado = 1 
      AND marca_baja = FALSE;

    IF v_nombre_producto IS NULL THEN
        RAISE EXCEPTION 'El registro pendiente no existe o ya fue procesado.';
    END IF;

    -- Prevencion de duplicidad por codigo de barras (ignora cadenas vacias o nulas)
    IF v_producto_id IS NULL AND v_codigo_barras IS NOT NULL AND TRIM(v_codigo_barras) <> '' THEN
        SELECT producto_id INTO v_producto_id 
        FROM productos 
        WHERE codigo_barras = TRIM(v_codigo_barras) AND marca_baja = FALSE 
        LIMIT 1;
    END IF;

    IF v_producto_id IS NULL THEN
        INSERT INTO productos 
            (categoria_id, codigo_barras, nombre_producto, codigo_interno, precio_compra, precio_venta, estado, fecha_creacion, usuario_creacion, marca_baja)
        VALUES 
            (v_categoria_id, NULLIF(TRIM(v_codigo_barras), ''), v_nombre_producto, v_codigo_interno, v_precio_compra, v_precio_venta, 1, CURRENT_TIMESTAMP, p_usuario_revision, FALSE)
        RETURNING producto_id INTO v_producto_id;
    ELSE
        IF v_codigo_barras IS NOT NULL AND TRIM(v_codigo_barras) <> '' AND EXISTS (
            SELECT 1 FROM productos 
            WHERE codigo_barras = TRIM(v_codigo_barras) AND producto_id <> v_producto_id AND marca_baja = FALSE
        ) THEN
            RAISE EXCEPTION 'El codigo de barras editado ya pertenece a otro producto distinto.';
        END IF;

        UPDATE productos
        SET nombre_producto = v_nombre_producto, 
            categoria_id = v_categoria_id, 
            codigo_barras = NULLIF(TRIM(v_codigo_barras), ''),
            precio_compra = v_precio_compra, 
            precio_venta = v_precio_venta, 
            fecha_modificacion = CURRENT_TIMESTAMP, 
            usuario_modificacion = p_usuario_revision
        WHERE producto_id = v_producto_id AND marca_baja = FALSE;
    END IF;

    INSERT INTO lotes
        (proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, fecha_vencimiento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (v_proveedor_id, COALESCE(v_descripcion_lote, 'Lote ' || v_codigo_interno), v_stock_ingresado, COALESCE(v_precio_lote, v_precio_compra * v_stock_ingresado), v_precio_compra, v_fecha_vencimiento, CURRENT_TIMESTAMP, p_usuario_revision, FALSE)
    RETURNING lote_id INTO v_nuevo_lote_id;

    INSERT INTO inventario
        (sucursal_id, lote_id, producto_id, stock, en_descuento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (v_sucursal_id, v_nuevo_lote_id, v_producto_id, v_stock_ingresado, FALSE, CURRENT_TIMESTAMP, p_usuario_revision, FALSE);

    UPDATE inventario_pendientes
    SET estado = 2, 
        producto_id = v_producto_id, 
        fecha_revision = CURRENT_TIMESTAMP, 
        usuario_revision = p_usuario_revision
    WHERE inventario_pendiente_id = p_inventario_pendiente_id;
END;
$$;
