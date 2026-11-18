-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Verifica en el acto la disponibilidad física de stock de un producto al
--                  intentar agregarlo o cerrar una venta en el terminal POS.
--                  Retorna si la venta puede proceder y el nivel de advertencia resultante.
-- ============================================================================================

DROP FUNCTION IF EXISTS fn_notificaciones_verificar_stock_venta(INT, INT, NUMERIC);
DROP FUNCTION IF EXISTS fn_notificaciones_verificar_stock_venta(INT, INT, INT);

CREATE OR REPLACE FUNCTION fn_notificaciones_verificar_stock_venta(
    p_sucursal_id INT,
    p_producto_id INT,
    p_cantidad_solicitada NUMERIC
)
RETURNS TABLE (
    puede_vender BOOLEAN,
    stock_actual INT,
    stock_restante NUMERIC,
    nivel_alerta VARCHAR,
    mensaje_alerta VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_stock_actual INT := 0;
    v_stock_restante NUMERIC := 0;
    v_nombre_prod VARCHAR := '';
    v_cod_interno VARCHAR := '';
    v_cod_sucursal VARCHAR := '';
BEGIN
    -- Obtener datos informativos del producto y sucursal
    SELECT COALESCE(p.nombre_producto, 'Producto #' || p_producto_id), COALESCE(p.codigo_interno, '')
    INTO v_nombre_prod, v_cod_interno
    FROM productos p
    WHERE p.producto_id = p_producto_id;

    SELECT COALESCE(s.codigo_interno, 'SUC-00' || p_sucursal_id)
    INTO v_cod_sucursal
    FROM sucursales s
    WHERE s.sucursal_id = p_sucursal_id;

    -- Calcular stock consolidado actual en la sucursal (excluyendo mermas / bajas)
    SELECT COALESCE(SUM(i.stock), 0)::INT
    INTO v_stock_actual
    FROM inventario i
    WHERE i.sucursal_id = p_sucursal_id
      AND i.producto_id = p_producto_id
      AND i.marca_baja = FALSE;

    -- Validar cantidad solicitada
    IF p_cantidad_solicitada IS NULL OR p_cantidad_solicitada <= 0 THEN
        RETURN QUERY SELECT
            FALSE,
            v_stock_actual,
            v_stock_actual::NUMERIC,
            'INVALIDO'::VARCHAR,
            'La cantidad solicitada para la venta debe ser mayor a 0.'::VARCHAR;
        RETURN;
    END IF;

    v_stock_restante := v_stock_actual - p_cantidad_solicitada;

    -- Caso A: Sin stock suficiente para satisfacer la venta
    IF v_stock_actual <= 0 THEN
        RETURN QUERY SELECT
            FALSE,
            v_stock_actual,
            v_stock_actual::NUMERIC,
            'SIN_STOCK'::VARCHAR,
            ('El producto ' || v_nombre_prod || ' está completamente agotado (Stock 0) en ' || v_cod_sucursal || '.')::VARCHAR;
        RETURN;
    ELSIF v_stock_restante < 0 THEN
        RETURN QUERY SELECT
            FALSE,
            v_stock_actual,
            v_stock_restante,
            'SIN_STOCK'::VARCHAR,
            ('Stock insuficiente en ' || v_cod_sucursal || '. Solicitadas: ' || p_cantidad_solicitada || ' u., Disponibles en estantería: ' || v_stock_actual || ' u.')::VARCHAR;
        RETURN;
    END IF;

    -- Caso B: La venta es viable (stock suficiente), pero evaluamos alertas post-venta
    IF v_stock_restante = 0 THEN
        RETURN QUERY SELECT
            TRUE,
            v_stock_actual,
            v_stock_restante,
            'AGOTADO_POST_VENTA'::VARCHAR,
            ('Atención: Esta venta agotará completamente el stock disponible de ' || v_nombre_prod || ' en ' || v_cod_sucursal || '.')::VARCHAR;
        RETURN;
    ELSIF v_stock_restante <= 10 THEN
        RETURN QUERY SELECT
            TRUE,
            v_stock_actual,
            v_stock_restante,
            'STOCK_BAJO_POST_VENTA'::VARCHAR,
            ('Advertencia: Tras esta transacción solo quedarán ' || v_stock_restante || ' u. disponibles de ' || v_nombre_prod || ' en ' || v_cod_sucursal || ' (umbral de stock crítico).')::VARCHAR;
        RETURN;
    ELSE
        RETURN QUERY SELECT
            TRUE,
            v_stock_actual,
            v_stock_restante,
            'OK'::VARCHAR,
            ('Stock disponible óptimo. Quedarán ' || v_stock_restante || ' unidades disponibles en ' || v_cod_sucursal || '.')::VARCHAR;
        RETURN;
    END IF;
END;
$$;
