-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     REGISTRA ARQUEO DE CAJA POR TURNO DE CAJERO (OPCION B CONSOLIDADA +
--                  SELLADO DE MOVIMIENTOS DEL TURNO + CIERRE ATOMICO DE SESION)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_reportes_registrar_arqueo(
    p_sesion_id INT,
    p_usuario_id INT,
    p_sucursal_id INT,
    p_monto_base DECIMAL(8,2),        -- Nullable: fondo inicial de caja
    p_datos_snapshot JSONB,           -- Indicadores congelados (esperado, declarado, diferencia, top)
    p_entradas_json JSONB,            -- Entradas declaradas por el cajero
    p_salidas_json JSONB,             -- Salidas declaradas por el cajero
    p_periodo VARCHAR(20) DEFAULT 'Turno',
    INOUT p_nuevo_reporte_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_fecha_inicio    TIMESTAMP;
    v_fecha_fin       TIMESTAMP := CURRENT_TIMESTAMP;
    v_entradas_json   JSONB := COALESCE(p_entradas_json, '[]'::JSONB);
    v_salidas_json    JSONB := COALESCE(p_salidas_json,  '[]'::JSONB);
    v_item            JSONB;

    v_total_entradas  NUMERIC(12,2) := 0;
    v_total_salidas   NUMERIC(12,2) := 0;
    v_mov_entradas    NUMERIC(12,2) := 0;
    v_mov_salidas     NUMERIC(12,2) := 0;
    v_duracion_sesion INT;
BEGIN
    -- 1. Validar que la sesión pertenezca al usuario y siga abierta
    SELECT s.hora_sesion_abierta INTO v_fecha_inicio
    FROM Sesiones s
    WHERE s.sesion_id = p_sesion_id
      AND s.usuario_id = p_usuario_id
      AND s.hora_sesion_cerrada IS NULL
      AND s.marca_baja = false
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe una sesión abierta válida (ID: %) para el usuario %.', p_sesion_id, p_usuario_id;
    END IF;

    IF jsonb_typeof(v_entradas_json) <> 'array' THEN
        RAISE EXCEPTION 'El parámetro p_entradas_json debe ser un arreglo JSON válido.';
    END IF;

    IF jsonb_typeof(v_salidas_json) <> 'array' THEN
        RAISE EXCEPTION 'El parámetro p_salidas_json debe ser un arreglo JSON válido.';
    END IF;

    -- 2. Crear cabecera del Reporte con la ventana exacta del turno
    INSERT INTO Reportes (
        sucursal_id, periodo, fecha_inicio, fecha_fin,
        monto_base, monto_entradas, monto_salidas, utilidad,
        datos_snapshot, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_sucursal_id, p_periodo, v_fecha_inicio, v_fecha_fin,
        p_monto_base, 0, 0, 0,
        COALESCE(p_datos_snapshot, '{}'::JSONB), false, v_fecha_fin, p_usuario_id
    )
    RETURNING reporte_id INTO p_nuevo_reporte_id;

    -- 3. Insertar Entradas declaradas por el cajero
    FOR v_item IN SELECT * FROM jsonb_array_elements(v_entradas_json)
    LOOP
        IF (v_item->>'monto')::DECIMAL(8,2) <= 0 THEN
            RAISE EXCEPTION 'Todas las entradas deben tener un monto mayor a 0.';
        END IF;

        INSERT INTO Entradas (
            reporte_id, tipo_entrada, monto, descripcion, marca_baja
        )
        VALUES (
            p_nuevo_reporte_id,
            (v_item->>'tipo_entrada')::INT,
            (v_item->>'monto')::DECIMAL(8,2),
            LEFT(TRIM(v_item->>'descripcion'), 40),
            false
        );

        v_total_entradas := v_total_entradas + (v_item->>'monto')::NUMERIC(12,2);
    END LOOP;

    -- 4. Insertar Salidas declaradas por el cajero
    FOR v_item IN SELECT * FROM jsonb_array_elements(v_salidas_json)
    LOOP
        IF (v_item->>'monto')::DECIMAL(8,2) <= 0 THEN
            RAISE EXCEPTION 'Todas las salidas deben tener un monto mayor a 0.';
        END IF;

        INSERT INTO Salidas (
            reporte_id, tipo_salida, monto, descripcion, marca_baja
        )
        VALUES (
            p_nuevo_reporte_id,
            (v_item->>'tipo_salida')::INT,
            (v_item->>'monto')::DECIMAL(8,2),
            LEFT(TRIM(v_item->>'descripcion'), 40),
            false
        );

        v_total_salidas := v_total_salidas + (v_item->>'monto')::NUMERIC(12,2);
    END LOOP;

    -- 5. Sellar SOLO los Movimientos de este cajero realizados durante este turno
    WITH sellados AS (
        UPDATE Movimientos m
        SET
            reporte_id           = p_nuevo_reporte_id,
            fecha_modificacion   = v_fecha_fin,
            usuario_modificacion = p_usuario_id
        WHERE m.usuario_creacion = p_usuario_id
          AND m.reporte_id IS NULL
          AND m.marca_baja = false
          AND m.fecha_creacion BETWEEN v_fecha_inicio AND v_fecha_fin
        RETURNING m.tipo_movimiento, m.monto
    )
    SELECT
        COALESCE(SUM(CASE WHEN tipo_movimiento = 1 THEN monto ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN tipo_movimiento = 2 THEN monto ELSE 0 END), 0)
    INTO v_mov_entradas, v_mov_salidas
    FROM sellados;

    -- 6. OPCION B: Insertar 1 fila consolidada en Entradas y/o Salidas por los Movimientos sellados
    IF v_mov_entradas > 0 THEN
        INSERT INTO Entradas (
            reporte_id, tipo_entrada, monto, descripcion, marca_baja
        )
        VALUES (
            p_nuevo_reporte_id,
            5, -- Grupo 4: 5 = Otro (el detalle por método queda en Movimientos)
            v_mov_entradas::DECIMAL(8,2),
            'Movimientos de entrada del turno',
            false
        );

        v_total_entradas := v_total_entradas + v_mov_entradas;
    END IF;

    IF v_mov_salidas > 0 THEN
        INSERT INTO Salidas (
            reporte_id, tipo_salida, monto, descripcion, marca_baja
        )
        VALUES (
            p_nuevo_reporte_id,
            5, -- Grupo 4: 5 = Otro
            v_mov_salidas::DECIMAL(8,2),
            'Movimientos de salida del turno',
            false
        );

        v_total_salidas := v_total_salidas + v_mov_salidas;
    END IF;

    -- 7. Actualizar totales en Reportes (sin doble conteo)
    UPDATE Reportes
    SET
        monto_entradas = v_total_entradas::DECIMAL(8,2),
        monto_salidas  = v_total_salidas::DECIMAL(8,2),
        utilidad       = (v_total_entradas - v_total_salidas)::DECIMAL(8,2)
    WHERE reporte_id = p_nuevo_reporte_id;

    -- 8. Cerrar sesión automáticamente en la misma transacción
    CALL sp_sesiones_cerrar(p_sesion_id, p_usuario_id, v_duracion_sesion);
END;
$$;