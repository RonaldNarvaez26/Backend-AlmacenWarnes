CREATE OR REPLACE PROCEDURE spInventarioPendientesAprobar(
    p_inventario_pendiente_id INT,
    p_usuario_revision INT,
    p_proveedor_id INT DEFAULT NULL,
    p_descripcion_lote VARCHAR(100) DEFAULT NULL,
    p_precio_lote DECIMAL(8,2) DEFAULT NULL,
    p_fecha_vencimiento DATE DEFAULT NULL
)
LANGUAGE plpgsql AS $$
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
    -- Seleccionar UNICAMENTE las columnas que existen fisicamente en inventario_pendientes
    SELECT producto_id, categoria_id, nombre_producto, codigo_barras, codigo_interno, 
           stock_ingresado, precio_compra, precio_venta
    INTO v_producto_id, v_categoria_id, v_nombre_producto, v_codigo_barras, v_codigo_interno,
         v_stock_ingresado, v_precio_compra, v_precio_venta
    FROM inventario_pendientes
    WHERE inventario_pendiente_id = p_inventario_pendiente_id
      AND marca_baja = FALSE
      AND estado IN (1, 4);

    IF v_nombre_producto IS NULL THEN
        RAISE EXCEPTION 'El registro pendiente con ID % no existe o ya fue procesado.', p_inventario_pendiente_id;
    END IF;

    -- Resolver datos del lote a partir de los parametros inyectados desde el DTO / servicio
    v_proveedor_id := p_proveedor_id;
    v_descripcion_lote := COALESCE(p_descripcion_lote, 'Lote ' || v_codigo_interno);
    v_precio_lote := COALESCE(p_precio_lote, v_precio_compra * v_stock_ingresado);
    v_fecha_vencimiento := p_fecha_vencimiento;

    -- Si no vino proveedor y el producto ya existia en inventario, consultar el proveedor del lote previo
    IF v_proveedor_id IS NULL AND v_producto_id IS NOT NULL THEN
        SELECT l.proveedor_id INTO v_proveedor_id
        FROM lotes l
        JOIN inventario i ON i.lote_id = l.lote_id
        WHERE i.producto_id = v_producto_id AND l.marca_baja = FALSE
        ORDER BY l.lote_id DESC
        LIMIT 1;
    END IF;

    -- Validar colision de codigo de barras en otro producto activo
    IF v_codigo_barras IS NOT NULL AND TRIM(v_codigo_barras) <> '' THEN
        IF EXISTS (
            SELECT 1 FROM productos
            WHERE codigo_barras = TRIM(v_codigo_barras)
              AND marca_baja = FALSE
              AND (v_producto_id IS NULL OR producto_id <> v_producto_id)
        ) THEN
            RAISE EXCEPTION 'El codigo de barras % ya se encuentra registrado en otro producto.', v_codigo_barras;
        END IF;
    END IF;

    -- Resolver ID de producto existente o nuevo
    IF v_producto_id IS NULL AND v_codigo_barras IS NOT NULL AND TRIM(v_codigo_barras) <> '' THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_barras = TRIM(v_codigo_barras) AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    IF v_producto_id IS NULL AND v_codigo_interno IS NOT NULL THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_interno = v_codigo_interno AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    -- Insertar o actualizar producto maestro (garantizando NOT NULL en codigo_barras segun esquema original)
    IF v_producto_id IS NULL THEN
        INSERT INTO productos (
            categoria_id, codigo_barras, nombre_producto, codigo_interno,
            precio_compra, precio_venta, estado, marca_baja, fecha_creacion, usuario_creacion
        ) VALUES (
            v_categoria_id,
            COALESCE(NULLIF(TRIM(v_codigo_barras), ''), v_codigo_interno),
            v_nombre_producto,
            v_codigo_interno,
            v_precio_compra,
            v_precio_venta,
            1,
            FALSE,
            CURRENT_TIMESTAMP,
            p_usuario_revision
        ) RETURNING producto_id INTO v_producto_id;
    ELSE
        UPDATE productos
        SET nombre_producto = v_nombre_producto,
            categoria_id = v_categoria_id,
            codigo_barras = COALESCE(NULLIF(TRIM(v_codigo_barras), ''), codigo_barras),
            precio_compra = v_precio_compra,
            precio_venta = v_precio_venta,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_revision
        WHERE producto_id = v_producto_id AND marca_baja = FALSE;
    END IF;

    -- Insertar lote con los datos inyectados por parametro
    INSERT INTO lotes (
        proveedor_id, descripcion, cantidad, precio_lote, costo_unitario,
        fecha_vencimiento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        v_proveedor_id,
        v_descripcion_lote,
        v_stock_ingresado,
        v_precio_lote,
        v_precio_compra,
        v_fecha_vencimiento,
        FALSE,
        CURRENT_TIMESTAMP,
        p_usuario_revision
    ) RETURNING lote_id INTO v_nuevo_lote_id;

    -- Insertar en inventario de sucursal
    INSERT INTO inventario (
        sucursal_id, lote_id, producto_id, stock, en_descuento,
        precio_descuento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        v_sucursal_id, v_nuevo_lote_id, v_producto_id, v_stock_ingresado, FALSE,
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
