CREATE OR REPLACE PROCEDURE spInventarioPendientesAprobar(
    p_inventario_pendiente_id INT,
    p_usuario_revision INT
)
LANGUAGE plpgsql AS $$
DECLARE
    v_reg RECORD;
    v_producto_id INT;
    v_lote_id INT;
BEGIN
    SELECT * INTO v_reg
    FROM inventario_pendientes
    WHERE inventario_pendiente_id = p_inventario_pendiente_id
      AND marca_baja = FALSE
      AND estado = 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El registro pendiente con ID % no existe o ya fue procesado.', p_inventario_pendiente_id;
    END IF;

    -- Validar colision de codigo de barras en otro producto activo
    IF v_reg.codigo_barras IS NOT NULL AND TRIM(v_reg.codigo_barras) <> '' THEN
        IF EXISTS (
            SELECT 1 FROM productos
            WHERE codigo_barras = TRIM(v_reg.codigo_barras)
              AND marca_baja = FALSE
              AND (v_reg.producto_id IS NULL OR producto_id <> v_reg.producto_id)
        ) THEN
            RAISE EXCEPTION 'El codigo de barras % ya se encuentra registrado en otro producto.', v_reg.codigo_barras;
        END IF;
    END IF;

    -- Resolver ID de producto existente o nuevo
    v_producto_id := v_reg.producto_id;
    IF v_producto_id IS NULL AND v_reg.codigo_barras IS NOT NULL AND TRIM(v_reg.codigo_barras) <> '' THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_barras = TRIM(v_reg.codigo_barras) AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    IF v_producto_id IS NULL THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_interno = v_reg.codigo_interno AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    -- Insertar o actualizar producto maestro
    IF v_producto_id IS NULL THEN
        INSERT INTO productos (
            categoria_id, codigo_barras, nombre_producto, codigo_interno,
            precio_compra, precio_venta, estado, marca_baja, fecha_creacion, usuario_creacion
        ) VALUES (
            v_reg.categoria_id,
            NULLIF(TRIM(v_reg.codigo_barras), ''),
            v_reg.nombre_producto,
            v_reg.codigo_interno,
            v_reg.precio_compra,
            v_reg.precio_venta,
            1,
            FALSE,
            CURRENT_TIMESTAMP,
            p_usuario_revision
        ) RETURNING producto_id INTO v_producto_id;
    ELSE
        -- Actualizar datos maestros si el supervisor corrigio datos del cajero
        UPDATE productos
        SET nombre_producto = v_reg.nombre_producto,
            categoria_id = v_reg.categoria_id,
            codigo_barras = NULLIF(TRIM(v_reg.codigo_barras), ''),
            precio_compra = v_reg.precio_compra,
            precio_venta = v_reg.precio_venta,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_revision
        WHERE producto_id = v_producto_id AND marca_baja = FALSE;
    END IF;

    -- Insertar lote garantizando NOT NULL en precio_lote y descripcion
    INSERT INTO lotes (
        proveedor_id, descripcion, cantidad, precio_lote, costo_unitario,
        fecha_vencimiento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        v_reg.proveedor_id,
        COALESCE(v_reg.descripcion_lote, 'Lote ' || v_reg.codigo_interno),
        v_reg.stock_ingresado,
        COALESCE(v_reg.precio_lote, v_reg.precio_compra * v_reg.stock_ingresado),
        v_reg.precio_compra,
        v_reg.fecha_vencimiento,
        FALSE,
        CURRENT_TIMESTAMP,
        p_usuario_revision
    ) RETURNING lote_id INTO v_lote_id;

    -- Insertar en inventario de sucursal
    INSERT INTO inventario (
        sucursal_id, lote_id, producto_id, stock, en_descuento,
        precio_descuento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        COALESCE(v_reg.sucursal_id, 1), v_lote_id, v_producto_id, v_reg.stock_ingresado, FALSE,
        NULL, FALSE, CURRENT_TIMESTAMP, p_usuario_revision
    );

    -- Actualizar estado a 2 (Aprobado)
    UPDATE inventario_pendientes
    SET estado = 2,
        producto_id = v_producto_id,
        fecha_revision = CURRENT_TIMESTAMP,
        usuario_revision = p_usuario_revision
    WHERE inventario_pendiente_id = p_inventario_pendiente_id;
END;
$$;
