-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     REGISTRA LA VENTA, SUS DETALLES Y DESCUENTA EL STOCK EN UNA SOLA TRANSACCIÓN
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_ventas_registrar(
    p_sucursal_id INT,
    p_usuario_id INT, -- El cajero
    p_cliente_id INT, -- Nullable
    p_metodo_venta INT, -- 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto
    p_precio_total DECIMAL(8,2),
    p_cobro_efectivo DECIMAL(8,2),
    p_cobro_qr DECIMAL(8,2),
    p_cobro_tarjeta DECIMAL(8,2),
    p_monto_recibido DECIMAL(8,2),
    p_cambio DECIMAL(8,2),
    p_detalles_json JSON, -- Estructura: [{"inventario_id": 1, "producto_id": 2, "cantidad": 1, "precio_unitario": 10.50}]
    INOUT p_nueva_venta_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_detalle JSON;
    v_inventario_id INT;
    v_producto_id INT;
    v_cantidad DECIMAL(8,2);
    v_precio_unitario DECIMAL(8,2);
    v_stock_actual INT;
BEGIN
    -- 1. Validar que la suma de los cobros mixtos coincida con el total
    IF (p_cobro_efectivo + p_cobro_qr + p_cobro_tarjeta) <> p_precio_total THEN
        RAISE EXCEPTION 'La suma de los métodos de pago no coincide con el precio total.';
    END IF;

    -- 2. Insertar la cabecera de la Venta (Estado 2 = Finalizada/Aprobada)
    INSERT INTO Ventas (
        sucursal_id, estado, precio_total, metodo_venta, 
        cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, 
        cliente_id, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        p_sucursal_id, 2, p_precio_total, p_metodo_venta,
        p_cobro_qr, p_cobro_efectivo, p_cobro_tarjeta, p_monto_recibido, p_cambio,
        p_cliente_id, false, CURRENT_TIMESTAMP, p_usuario_id
    ) RETURNING venta_id INTO p_nueva_venta_id;

    -- 3. Iterar sobre el JSON para procesar el carrito
    FOR v_detalle IN SELECT * FROM json_array_elements(p_detalles_json)
    LOOP
        v_inventario_id := (v_detalle->>'inventario_id')::INT;
        v_producto_id := (v_detalle->>'producto_id')::INT;
        v_cantidad := (v_detalle->>'cantidad')::DECIMAL;
        v_precio_unitario := (v_detalle->>'precio_unitario')::DECIMAL;

        -- 3.1 Bloquear la fila del inventario para evitar concurrencia (Row-level lock)
        SELECT stock INTO v_stock_actual 
        FROM Inventario 
        WHERE inventario_id = v_inventario_id FOR UPDATE;

        -- 3.2 Hard Stop: Verificar que alcance el stock en tiempo real
        IF v_stock_actual < v_cantidad THEN
            RAISE EXCEPTION 'Stock insuficiente para el inventario ID: %', v_inventario_id;
        END IF;

        -- 3.3 Descontar el stock
        UPDATE Inventario 
        SET stock = stock - v_cantidad,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_id
        WHERE inventario_id = v_inventario_id;

        -- 3.4 Registrar el detalle
        INSERT INTO Detalles_Ventas (
            producto_id, venta_id, inventario_id, precio_unitario, cantidad, marca_baja
        ) VALUES (
            v_producto_id, p_nueva_venta_id, v_inventario_id, v_precio_unitario, v_cantidad, false
        );
    END LOOP;
END;
$$;