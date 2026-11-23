-- ============================================================================================
-- AUTOR:           Antigravity / Almacen Warnes
-- DESCRIPCION:     RESUMEN CONSOLIDADO DEL LIBRO MAYOR CON FILTROS AVANZADOS
--                  (SUCURSAL, FECHAS, HORAS, CAJERO, PRODUCTO)
-- ============================================================================================
DROP FUNCTION IF EXISTS fn_libro_mayor_resumen(INT, DATE, DATE);
DROP FUNCTION IF EXISTS fn_libro_mayor_resumen(INT, DATE, DATE, TIME, TIME, INT, INT);

CREATE OR REPLACE FUNCTION fn_libro_mayor_resumen(
    p_sucursal_id INT DEFAULT NULL,
    p_fecha_inicio DATE DEFAULT CURRENT_DATE,
    p_fecha_fin DATE DEFAULT CURRENT_DATE,
    p_hora_inicio TIME DEFAULT NULL,
    p_hora_fin TIME DEFAULT NULL,
    p_cajero_id INT DEFAULT NULL,
    p_producto_id INT DEFAULT NULL
)
RETURNS TABLE (
    total_ventas_qr NUMERIC(12,2),
    total_ventas_efectivo NUMERIC(12,2),
    total_ventas_tarjeta NUMERIC(12,2),
    total_ventas NUMERIC(12,2),
    total_entradas_extra NUMERIC(12,2),
    total_salidas_extra NUMERIC(12,2),
    total_entradas NUMERIC(12,2),
    total_salidas NUMERIC(12,2),
    total_bajas_monto NUMERIC(12,2),
    cantidad_bajas INT,
    saldo_neto NUMERIC(12,2)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_ventas_qr NUMERIC(12,2) := 0;
    v_ventas_efectivo NUMERIC(12,2) := 0;
    v_ventas_tarjeta NUMERIC(12,2) := 0;
    v_ventas_total NUMERIC(12,2) := 0;

    v_extra_entradas NUMERIC(12,2) := 0;
    v_extra_salidas NUMERIC(12,2) := 0;

    v_bajas_ventas_monto NUMERIC(12,2) := 0;
    v_bajas_ventas_cant INT := 0;

    v_bajas_movs_monto NUMERIC(12,2) := 0;
    v_bajas_movs_cant INT := 0;

    v_total_entradas NUMERIC(12,2) := 0;
    v_total_salidas NUMERIC(12,2) := 0;
    v_total_bajas_monto NUMERIC(12,2) := 0;
    v_cantidad_bajas INT := 0;
    v_saldo_neto NUMERIC(12,2) := 0;
BEGIN
    -- 1. Ventas Activas (no anuladas ni dadas de baja)
    SELECT
        COALESCE(SUM(v.cobro_qr), 0),
        COALESCE(SUM(v.cobro_efectivo), 0),
        COALESCE(SUM(v.cobro_tarjeta), 0),
        COALESCE(SUM(v.precio_total), 0)
    INTO
        v_ventas_qr,
        v_ventas_efectivo,
        v_ventas_tarjeta,
        v_ventas_total
    FROM Ventas v
    WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR v.sucursal_id = p_sucursal_id)
      AND (p_fecha_inicio IS NULL OR v.fecha_creacion::DATE >= p_fecha_inicio)
      AND (p_fecha_fin IS NULL OR v.fecha_creacion::DATE <= p_fecha_fin)
      AND (p_hora_inicio IS NULL OR v.fecha_creacion::TIME >= p_hora_inicio)
      AND (p_hora_fin IS NULL OR v.fecha_creacion::TIME <= p_hora_fin)
      AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR v.usuario_creacion = p_cajero_id)
      AND (p_producto_id IS NULL OR p_producto_id <= 0 OR EXISTS (
          SELECT 1 FROM Detalles_Ventas dv WHERE dv.venta_id = v.venta_id AND dv.producto_id = p_producto_id
      ))
      AND v.marca_baja = FALSE
      AND v.estado <> 3;

    -- 2. Ventas Dadas de Baja / Anuladas
    SELECT
        COALESCE(SUM(v.precio_total), 0),
        COUNT(*)::INT
    INTO
        v_bajas_ventas_monto,
        v_bajas_ventas_cant
    FROM Ventas v
    WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR v.sucursal_id = p_sucursal_id)
      AND (p_fecha_inicio IS NULL OR v.fecha_creacion::DATE >= p_fecha_inicio)
      AND (p_fecha_fin IS NULL OR v.fecha_creacion::DATE <= p_fecha_fin)
      AND (p_hora_inicio IS NULL OR v.fecha_creacion::TIME >= p_hora_inicio)
      AND (p_hora_fin IS NULL OR v.fecha_creacion::TIME <= p_hora_fin)
      AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR v.usuario_creacion = p_cajero_id)
      AND (p_producto_id IS NULL OR p_producto_id <= 0 OR EXISTS (
          SELECT 1 FROM Detalles_Ventas dv WHERE dv.venta_id = v.venta_id AND dv.producto_id = p_producto_id
      ))
      AND (v.marca_baja = TRUE OR v.estado = 3);

    -- 3. Movimientos Extra-Venta Activos (sólo si no se filtra por producto específico)
    IF p_producto_id IS NULL OR p_producto_id <= 0 THEN
        SELECT
            COALESCE(SUM(CASE WHEN m.tipo_movimiento = 1 THEN m.monto ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN m.tipo_movimiento = 2 THEN m.monto ELSE 0 END), 0)
        INTO
            v_extra_entradas,
            v_extra_salidas
        FROM Movimientos m
        LEFT JOIN Usuarios u ON m.usuario_creacion = u.usuario_id
        WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR COALESCE(m.sucursal_id, u.sucursal_id) = p_sucursal_id)
          AND (p_fecha_inicio IS NULL OR m.fecha_creacion::DATE >= p_fecha_inicio)
          AND (p_fecha_fin IS NULL OR m.fecha_creacion::DATE <= p_fecha_fin)
          AND (p_hora_inicio IS NULL OR m.fecha_creacion::TIME >= p_hora_inicio)
          AND (p_hora_fin IS NULL OR m.fecha_creacion::TIME <= p_hora_fin)
          AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR m.usuario_creacion = p_cajero_id)
          AND m.marca_baja = FALSE;

        -- 4. Movimientos Extra-Venta Dados de Baja
        SELECT
            COALESCE(SUM(m.monto), 0),
            COUNT(*)::INT
        INTO
            v_bajas_movs_monto,
            v_bajas_movs_cant
        FROM Movimientos m
        LEFT JOIN Usuarios u ON m.usuario_creacion = u.usuario_id
        WHERE (p_sucursal_id IS NULL OR p_sucursal_id <= 0 OR COALESCE(m.sucursal_id, u.sucursal_id) = p_sucursal_id)
          AND (p_fecha_inicio IS NULL OR m.fecha_creacion::DATE >= p_fecha_inicio)
          AND (p_fecha_fin IS NULL OR m.fecha_creacion::DATE <= p_fecha_fin)
          AND (p_hora_inicio IS NULL OR m.fecha_creacion::TIME >= p_hora_inicio)
          AND (p_hora_fin IS NULL OR m.fecha_creacion::TIME <= p_hora_fin)
          AND (p_cajero_id IS NULL OR p_cajero_id <= 0 OR m.usuario_creacion = p_cajero_id)
          AND m.marca_baja = TRUE;
    END IF;

    -- Cálculos consolidados
    v_total_entradas := v_ventas_total + v_extra_entradas;
    v_total_salidas := v_extra_salidas;
    v_total_bajas_monto := v_bajas_ventas_monto + v_bajas_movs_monto;
    v_cantidad_bajas := v_bajas_ventas_cant + v_bajas_movs_cant;
    v_saldo_neto := v_total_entradas - v_total_salidas;

    RETURN QUERY SELECT
        v_ventas_qr,
        v_ventas_efectivo,
        v_ventas_tarjeta,
        v_ventas_total,
        v_extra_entradas,
        v_extra_salidas,
        v_total_entradas,
        v_total_salidas,
        v_total_bajas_monto,
        v_cantidad_bajas,
        v_saldo_neto;
END;
$$;
