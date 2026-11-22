DROP PROCEDURE IF EXISTS spInventarioIngresoDirectoSupervisor(INT, INT, VARCHAR, VARCHAR, VARCHAR, INT, VARCHAR, INT, DECIMAL, DECIMAL, DECIMAL, DATE, INT);
DROP PROCEDURE IF EXISTS spInventarioIngresoDirectoSupervisor(INT, INT, VARCHAR, VARCHAR, VARCHAR, INT, VARCHAR, INT, DECIMAL, DECIMAL, DECIMAL, DATE, INT, INT);

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
    p_usuario_creacion INT,
    p_sucursal_id INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nuevo_lote_id INT;
    v_sucursal_id INT := COALESCE(p_sucursal_id, 1);
    v_stock_anterior INT := 0;
    v_stock_resultante INT := 0;
BEGIN
    -- Si no viene producto_id pero viene codigo_barras no vacio, buscar coincidencia
    IF p_producto_id IS NULL AND p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' THEN
        SELECT producto_id INTO p_producto_id 
        FROM productos 
        WHERE codigo_barras = TRIM(p_codigo_barras) AND marca_baja = FALSE 
        LIMIT 1;
    END IF;

    -- Si aun es NULL, buscar por codigo_interno
    IF p_producto_id IS NULL AND p_codigo_interno IS NOT NULL THEN
        SELECT producto_id INTO p_producto_id
        FROM productos
        WHERE codigo_interno = p_codigo_interno AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    -- Insertar o actualizar producto garantizando NOT NULL en codigo_barras segun esquema original
    IF p_producto_id IS NULL THEN
        INSERT INTO productos 
            (categoria_id, codigo_barras, nombre_producto, codigo_interno, precio_compra, precio_venta, estado, fecha_creacion, usuario_creacion, marca_baja)
        VALUES 
            (p_categoria_id, COALESCE(NULLIF(TRIM(p_codigo_barras), ''), p_codigo_interno), p_nombre_producto, p_codigo_interno, p_precio_compra, p_precio_venta, 1, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE)
        RETURNING producto_id INTO p_producto_id;
    ELSE
        IF p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' AND EXISTS (
            SELECT 1 FROM productos 
            WHERE codigo_barras = TRIM(p_codigo_barras) AND producto_id <> p_producto_id AND marca_baja = FALSE
        ) THEN
            RAISE EXCEPTION 'El codigo de barras % ya pertenece a otro producto distinto.', p_codigo_barras;
        END IF;

        UPDATE productos
        SET nombre_producto = p_nombre_producto, 
            categoria_id = p_categoria_id, 
            codigo_barras = COALESCE(NULLIF(TRIM(p_codigo_barras), ''), codigo_barras),
            precio_compra = p_precio_compra, 
            precio_venta = p_precio_venta, 
            fecha_modificacion = CURRENT_TIMESTAMP, 
            usuario_modificacion = p_usuario_creacion
        WHERE producto_id = p_producto_id AND marca_baja = FALSE;
    END IF;

    INSERT INTO lotes
        (proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, fecha_vencimiento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (p_proveedor_id, 
         COALESCE(p_descripcion_lote, 'Lote ' || p_codigo_interno), 
         p_stock_ingresado, 
         COALESCE(p_precio_lote, p_precio_compra * p_stock_ingresado), 
         p_precio_compra, 
         p_fecha_vencimiento, 
         CURRENT_TIMESTAMP, 
         p_usuario_creacion, 
         FALSE)
    RETURNING lote_id INTO v_nuevo_lote_id;

    -- Calcular stock previo del producto en la sucursal antes de asentar el nuevo lote
    SELECT COALESCE(SUM(stock), 0) INTO v_stock_anterior
    FROM inventario
    WHERE producto_id = p_producto_id AND sucursal_id = v_sucursal_id AND marca_baja = FALSE;

    v_stock_resultante := v_stock_anterior + p_stock_ingresado;

    INSERT INTO inventario
        (sucursal_id, lote_id, producto_id, stock, en_descuento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (v_sucursal_id, v_nuevo_lote_id, p_producto_id, p_stock_ingresado, FALSE, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE);

    -- Registrar movimiento en kardex historial_inventario (Tipo 1: Ingreso)
    INSERT INTO historial_inventario (
        sucursal_id, producto_id, tipo_movimiento,
        stock_anterior, stock_resultante,
        descripcion, marca_baja, fecha_creacion,
        usuario_creacion, usuario_autorizacion
    ) VALUES (
        v_sucursal_id, p_producto_id, 1, -- 1 = Ingreso
        v_stock_anterior, v_stock_resultante,
        COALESCE('Ingreso directo - ' || p_descripcion_lote, 'Ingreso directo de lote'),
        FALSE, CURRENT_TIMESTAMP,
        p_usuario_creacion, p_usuario_creacion
    );

    -- Registrar en inventario_pendientes con estado 2 (Aprobado) para auditoria y trazabilidad historica
    INSERT INTO inventario_pendientes (
        sucursal_id,
        producto_id,
        nombre_producto,
        codigo_barras,
        codigo_interno,
        categoria_id,
        precio_compra,
        precio_venta,
        stock_ingresado,
        estado,
        marca_baja,
        fecha_creacion,
        usuario_creacion,
        fecha_revision,
        usuario_revision,
        proveedor_id,
        descripcion_lote,
        precio_lote,
        fecha_vencimiento,
        observacion_revision
    ) VALUES (
        v_sucursal_id,
        p_producto_id,
        p_nombre_producto,
        COALESCE(NULLIF(TRIM(p_codigo_barras), ''), p_codigo_interno),
        p_codigo_interno,
        p_categoria_id,
        p_precio_compra,
        p_precio_venta,
        p_stock_ingresado,
        2, -- 2 = Aprobado (Conceptos Grupo 7)
        FALSE,
        CURRENT_TIMESTAMP,
        p_usuario_creacion,
        CURRENT_TIMESTAMP,
        p_usuario_creacion,
        p_proveedor_id,
        COALESCE(p_descripcion_lote, 'Lote ' || p_codigo_interno),
        COALESCE(p_precio_lote, p_precio_compra * p_stock_ingresado),
        p_fecha_vencimiento,
        'Ingreso directo aprobado por Supervisor/Propietario'
    );
END;
$$;
