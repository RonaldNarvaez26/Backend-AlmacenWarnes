CREATE OR REPLACE PROCEDURE spInventarioDescontarUnidades(
    p_inventario_id INT,
    p_cantidad_a_restar INT,
    p_usuario_id INT DEFAULT 1,
    p_motivo VARCHAR DEFAULT 'Ajuste de stock'
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_stock_actual INT;
    v_lote_id INT;
    v_producto_id INT;
    v_marca_baja BOOLEAN;
    v_nuevo_stock INT;
BEGIN
    -- 1. Validar que la cantidad sea un entero positivo
    IF p_cantidad_a_restar IS NULL OR p_cantidad_a_restar <= 0 THEN
        RAISE EXCEPTION 'La cantidad a descontar debe ser mayor a cero.';
    END IF;

    -- 2. Consultar el registro actual en inventario
    SELECT i.stock, i.lote_id, i.producto_id, i.marca_baja
    INTO v_stock_actual, v_lote_id, v_producto_id, v_marca_baja
    FROM inventario i
    WHERE i.inventario_id = p_inventario_id;

    IF v_stock_actual IS NULL THEN
        RAISE EXCEPTION 'El registro de inventario con ID % no existe.', p_inventario_id;
    END IF;

    IF v_marca_baja = TRUE THEN
        RAISE EXCEPTION 'El registro de inventario con ID % se encuentra dado de baja.', p_inventario_id;
    END IF;

    -- 3. Validar disponibilidad de stock
    IF p_cantidad_a_restar > v_stock_actual THEN
        RAISE EXCEPTION 'La cantidad a descontar supera el stock disponible';
    END IF;

    -- 4. Calcular nuevo stock
    v_nuevo_stock := v_stock_actual - p_cantidad_a_restar;

    -- 5. Actualizar el stock en la tabla inventario
    UPDATE inventario
    SET stock = v_nuevo_stock,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = COALESCE(p_usuario_id, 1)
    WHERE inventario_id = p_inventario_id;

    -- 6. Ajustar la cantidad en la tabla lotes correspondiente
    IF v_lote_id IS NOT NULL THEN
        UPDATE lotes
        SET cantidad = GREATEST(0, cantidad - p_cantidad_a_restar)
        WHERE lote_id = v_lote_id;
    END IF;
END;
$$;
