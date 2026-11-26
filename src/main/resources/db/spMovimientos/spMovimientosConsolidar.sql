-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     CONSOLIDA UNO O VARIOS MOVIMIENTOS DE CAJA PENDIENTES (ADMINISTRATIVOS O DE
--                  TURNOS CERRADOS SIN ARQUEO) GENERANDO UN REPORTE EN LIBRO MAYOR
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_movimientos_consolidar(
    p_sucursal_id INT,
    p_movimiento_ids INT[],
    p_usuario_id INT,
    INOUT p_nuevo_reporte_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_fecha_inicio    TIMESTAMP;
    v_fecha_fin       TIMESTAMP := CURRENT_TIMESTAMP;
    v_total_entradas  NUMERIC(12,2) := 0;
    v_total_salidas   NUMERIC(12,2) := 0;
    v_cantidad        INT := 0;
    v_mov             RECORD;
BEGIN
    IF p_movimiento_ids IS NULL OR COALESCE(array_length(p_movimiento_ids, 1), 0) = 0 THEN
        RAISE EXCEPTION 'Debe seleccionar al menos un movimiento pendiente para consolidar.';
    END IF;

    -- Bloquear movimientos válidos pertenecientes a la sucursal y sin reporte previo
    SELECT
        COUNT(*),
        MIN(m.fecha_creacion)
    INTO v_cantidad, v_fecha_inicio
    FROM Movimientos m
    WHERE m.movimiento_id = ANY(p_movimiento_ids)
      AND m.sucursal_id = p_sucursal_id
      AND m.reporte_id IS NULL
      AND m.marca_baja = false;

    IF v_cantidad = 0 THEN
        RAISE EXCEPTION 'No se encontraron movimientos pendientes válidos en la sucursal seleccionada.';
    END IF;

    -- Crear cabecera del Reporte administrativo/consolidado
    INSERT INTO Reportes (
        sucursal_id, periodo, fecha_inicio, fecha_fin,
        monto_base, monto_entradas, monto_salidas, utilidad,
        datos_snapshot, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_sucursal_id,
        'Administrativo',
        v_fecha_inicio,
        v_fecha_fin,
        NULL,
        0,
        0,
        0,
        jsonb_build_object(
            'tipo', 'ConsolidacionMovimientos',
            'cantidadMovimientos', v_cantidad
        ),
        false,
        v_fecha_fin,
        p_usuario_id
    )
    RETURNING reporte_id INTO p_nuevo_reporte_id;

    -- Registrar cada movimiento en Entradas / Salidas respetando su método y detalle
    FOR v_mov IN
        SELECT
            m.movimiento_id,
            m.monto,
            m.tipo_movimiento,
            m.metodo_movimiento,
            COALESCE(NULLIF(TRIM(m.detalle), ''), 'Movimiento de caja') AS detalle
        FROM Movimientos m
        WHERE m.movimiento_id = ANY(p_movimiento_ids)
          AND m.sucursal_id = p_sucursal_id
          AND m.reporte_id IS NULL
          AND m.marca_baja = false
        ORDER BY m.fecha_creacion ASC
        FOR UPDATE
    LOOP
        IF v_mov.tipo_movimiento = 1 THEN
            INSERT INTO Entradas (reporte_id, tipo_entrada, monto, descripcion, marca_baja)
            VALUES (p_nuevo_reporte_id, v_mov.metodo_movimiento, v_mov.monto, LEFT(v_mov.detalle, 40), false);
            v_total_entradas := v_total_entradas + v_mov.monto;
        ELSIF v_mov.tipo_movimiento = 2 THEN
            INSERT INTO Salidas (reporte_id, tipo_salida, monto, descripcion, marca_baja)
            VALUES (p_nuevo_reporte_id, v_mov.metodo_movimiento, v_mov.monto, LEFT(v_mov.detalle, 40), false);
            v_total_salidas := v_total_salidas + v_mov.monto;
        END IF;

        UPDATE Movimientos
        SET
            reporte_id           = p_nuevo_reporte_id,
            fecha_modificacion   = v_fecha_fin,
            usuario_modificacion = p_usuario_id
        WHERE movimiento_id = v_mov.movimiento_id;
    END LOOP;

    -- Actualizar totales finales en Reportes
    UPDATE Reportes
    SET
        monto_entradas = v_total_entradas::DECIMAL(8,2),
        monto_salidas  = v_total_salidas::DECIMAL(8,2),
        utilidad       = (v_total_entradas - v_total_salidas)::DECIMAL(8,2)
    WHERE reporte_id = p_nuevo_reporte_id;
END;
$$;
