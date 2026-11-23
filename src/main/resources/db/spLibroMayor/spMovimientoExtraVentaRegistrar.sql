-- ============================================================================================
-- AUTOR:           Antigravity / Almacen Warnes
-- DESCRIPCION:     REGISTRA UN MOVIMIENTO FINANCIERO EXTRA-VENTA (PRESTAMO, ADELANTO, GASTO, ETC.)
-- ============================================================================================
DROP PROCEDURE IF EXISTS sp_movimiento_extra_venta_registrar(INT, INT, INT, INT, NUMERIC, VARCHAR, INT, INT, VARCHAR);

CREATE OR REPLACE PROCEDURE sp_movimiento_extra_venta_registrar(
    IN p_sucursal_id INT,
    IN p_tipo_movimiento INT,       -- 1 = Entrada, 2 = Salida
    IN p_categoria_id INT,          -- Grupo 14 o Grupo 12/13
    IN p_metodo_pago INT,           -- Grupo 4: 1=Efectivo, 2=QR, 3=Tarjeta, etc.
    IN p_monto NUMERIC(12,2),
    IN p_detalle VARCHAR(40),
    IN p_usuario_id INT,
    OUT p_movimiento_id INT,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_reporte_id INT;
    v_detalle_trunc VARCHAR(40);
BEGIN
    IF p_sucursal_id IS NULL OR p_sucursal_id <= 0 THEN
        RAISE EXCEPTION 'Debe especificar una sucursal válida.';
    END IF;

    IF p_monto IS NULL OR p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto debe ser un valor positivo mayor a 0.';
    END IF;

    IF p_tipo_movimiento NOT IN (1, 2) THEN
        RAISE EXCEPTION 'El tipo de movimiento debe ser 1 (Entrada) o 2 (Salida).';
    END IF;

    v_detalle_trunc := SUBSTRING(TRIM(COALESCE(p_detalle, 'Movimiento Extra-Venta')), 1, 40);

    -- Asegurar o buscar un reporte base del día para la sucursal (para FKs de Entradas y Salidas)
    SELECT reporte_id INTO v_reporte_id
    FROM Reportes
    WHERE sucursal_id = p_sucursal_id
      AND fecha_inicio = CURRENT_DATE
      AND marca_baja = FALSE
    LIMIT 1;

    IF v_reporte_id IS NULL THEN
        INSERT INTO Reportes (
            sucursal_id, periodo, fecha_inicio, fecha_fin,
            monto_base, monto_entradas, monto_salidas, utilidad,
            datos_snapshot, marca_baja, fecha_creacion, usuario_creacion
        ) VALUES (
            p_sucursal_id, 'Diario', CURRENT_DATE, CURRENT_DATE,
            0.00, 0.00, 0.00, 0.00,
            '{}'::jsonb, FALSE, CURRENT_TIMESTAMP, p_usuario_id
        ) RETURNING reporte_id INTO v_reporte_id;
    END IF;

    -- 1. Insertar en Movimientos
    INSERT INTO Movimientos (
        sucursal_id, reporte_id, monto, tipo_movimiento, metodo_movimiento,
        detalle, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        p_sucursal_id, v_reporte_id, p_monto, p_tipo_movimiento, COALESCE(p_metodo_pago, 1),
        v_detalle_trunc, FALSE, CURRENT_TIMESTAMP, p_usuario_id
    ) RETURNING movimiento_id INTO p_movimiento_id;

    -- 2. Reflejar en Entradas o Salidas según corresponda
    IF p_tipo_movimiento = 1 THEN
        INSERT INTO Entradas (
            reporte_id, tipo_entrada, monto, descripcion, marca_baja
        ) VALUES (
            v_reporte_id, COALESCE(p_categoria_id, 2), p_monto, v_detalle_trunc, FALSE
        );
    ELSE
        INSERT INTO Salidas (
            reporte_id, tipo_salida, monto, descripcion, marca_baja
        ) VALUES (
            v_reporte_id, COALESCE(p_categoria_id, 3), p_monto, v_detalle_trunc, FALSE
        );
    END IF;

    p_mensaje := 'Movimiento extra-venta registrado exitosamente con ID: ' || p_movimiento_id;
END;
$$;
