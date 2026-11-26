-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Senior Full-Stack Engineer
-- DESCRIPCION:     GENERA UN REPORTE FINANCIERO CONSOLIDADO DE AUDITORIA POR PERIODO
--                  (DIARIO, SEMANAL, MENSUAL, ANUAL O PERSONALIZADO) PARA UNA SUCURSAL
--                  SIN ALTERAR SESIONES NI SELLAR MOVIMIENTOS INDIVIDUALES DE TURNO
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_reportes_generar_periodo(INT, VARCHAR, TIMESTAMP, TIMESTAMP, INT, INT);
DROP PROCEDURE IF EXISTS sp_reportes_generar_periodo(INT, VARCHAR, TIMESTAMP, TIMESTAMP, INT);

CREATE OR REPLACE PROCEDURE sp_reportes_generar_periodo(
    p_sucursal_id INT,
    p_periodo VARCHAR(20),
    p_fecha_inicio TIMESTAMP,
    p_fecha_fin TIMESTAMP,
    p_usuario_id INT,
    INOUT p_nuevo_reporte_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_periodo_limpio   VARCHAR(20);
    v_conteo_ventas    INT := 0;
    v_tot_efectivo     NUMERIC(12,2) := 0;
    v_tot_qr           NUMERIC(12,2) := 0;
    v_tot_tarjeta      NUMERIC(12,2) := 0;
    v_tot_ventas       NUMERIC(12,2) := 0;

    v_cant_movimientos INT := 0;
    v_mov_entradas     NUMERIC(12,2) := 0;
    v_mov_salidas      NUMERIC(12,2) := 0;

    v_total_entradas   NUMERIC(12,2) := 0;
    v_total_salidas    NUMERIC(12,2) := 0;
    v_utilidad         NUMERIC(12,2) := 0;

    v_top_productos    JSONB := '[]'::JSONB;
    v_snapshot         JSONB;
BEGIN
    -- 1. Validaciones de parametros
    IF p_sucursal_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM Sucursales WHERE sucursal_id = p_sucursal_id AND marca_baja = false
    ) THEN
        RAISE EXCEPTION 'La sucursal seleccionada (ID: %) no existe o se encuentra inactiva.', p_sucursal_id;
    END IF;

    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'Debe especificar fecha y hora de inicio y fin para el corte de período.';
    END IF;

    IF p_fecha_inicio > p_fecha_fin THEN
        RAISE EXCEPTION 'La fecha de inicio (%) no puede ser posterior a la fecha de fin (%).', p_fecha_inicio, p_fecha_fin;
    END IF;

    v_periodo_limpio := COALESCE(NULLIF(TRIM(p_periodo), ''), 'Personalizado');

    -- 2. Consolidar ventas aprobadas dentro del rango
    SELECT
        COUNT(*)::INT,
        COALESCE(SUM(v.cobro_efectivo), 0)::NUMERIC(12,2),
        COALESCE(SUM(v.cobro_qr), 0)::NUMERIC(12,2),
        COALESCE(SUM(v.cobro_tarjeta), 0)::NUMERIC(12,2),
        COALESCE(SUM(v.precio_total), 0)::NUMERIC(12,2)
    INTO
        v_conteo_ventas,
        v_tot_efectivo,
        v_tot_qr,
        v_tot_tarjeta,
        v_tot_ventas
    FROM Ventas v
    WHERE v.sucursal_id = p_sucursal_id
      AND v.estado = 2 -- Finalizada / Aprobada
      AND v.marca_baja = false
      AND v.fecha_creacion BETWEEN p_fecha_inicio AND p_fecha_fin;

    -- 3. Consolidar movimientos de caja activos dentro del rango (sin alterar su estado de sellado)
    SELECT
        COUNT(*)::INT,
        COALESCE(SUM(CASE WHEN m.tipo_movimiento = 1 THEN m.monto ELSE 0 END), 0)::NUMERIC(12,2),
        COALESCE(SUM(CASE WHEN m.tipo_movimiento = 2 THEN m.monto ELSE 0 END), 0)::NUMERIC(12,2)
    INTO
        v_cant_movimientos,
        v_mov_entradas,
        v_mov_salidas
    FROM Movimientos m
    WHERE m.sucursal_id = p_sucursal_id
      AND m.marca_baja = false
      AND m.fecha_creacion BETWEEN p_fecha_inicio AND p_fecha_fin;

    -- 4. Obtener top 5 productos mas vendidos en el rango
    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'producto', rp.producto,
                'cantidad', rp.cantidad,
                'subtotal', rp.subtotal
            )
        ),
        '[]'::JSONB
    )
    INTO v_top_productos
    FROM (
        SELECT
            p.nombre_producto AS producto,
            SUM(dv.cantidad)::INT AS cantidad,
            COALESCE(SUM(dv.cantidad * dv.precio_unitario), 0)::NUMERIC(12,2) AS subtotal
        FROM Detalles_Ventas dv
        INNER JOIN Ventas v    ON dv.venta_id = v.venta_id
        INNER JOIN Productos p ON dv.producto_id = p.producto_id
        WHERE v.sucursal_id = p_sucursal_id
          AND v.estado = 2
          AND v.marca_baja = false
          AND dv.marca_baja = false
          AND v.fecha_creacion BETWEEN p_fecha_inicio AND p_fecha_fin
        GROUP BY p.producto_id, p.nombre_producto
        ORDER BY SUM(dv.cantidad) DESC
        LIMIT 5
    ) rp;

    -- 5. Calcular totales consolidados
    v_total_entradas := v_tot_ventas + v_mov_entradas;
    v_total_salidas  := v_mov_salidas;
    v_utilidad       := v_total_entradas - v_total_salidas;

    -- 6. Construir snapshot estructurado y compatible con el modal de auditoria
    v_snapshot := jsonb_build_object(
        'tipo', 'CortePeriodo',
        'periodo', v_periodo_limpio,
        'sucursalId', p_sucursal_id,
        'rango', jsonb_build_object(
            'fechaInicio', p_fecha_inicio,
            'fechaFin', p_fecha_fin
        ),
        'resumenVentas', jsonb_build_object(
            'conteo', v_conteo_ventas,
            'efectivo', v_tot_efectivo,
            'qr', v_tot_qr,
            'tarjeta', v_tot_tarjeta,
            'total', v_tot_ventas
        ),
        'resumenMovimientos', jsonb_build_object(
            'cantidad', v_cant_movimientos,
            'entradas', v_mov_entradas,
            'salidas', v_mov_salidas
        ),
        'totalesArqueo', jsonb_build_object(
            'entradasDeclaradas', v_total_entradas,
            'salidasDeclaradas', v_total_salidas,
            'utilidad', v_utilidad,
            'diferencia', 0.00
        ),
        'topProductos', v_top_productos
    );

    -- 7. Insertar registro cabecera en Reportes
    INSERT INTO Reportes (
        sucursal_id, periodo, fecha_inicio, fecha_fin,
        monto_base, monto_entradas, monto_salidas, utilidad,
        datos_snapshot, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_sucursal_id,
        v_periodo_limpio,
        p_fecha_inicio,
        p_fecha_fin,
        0.00,
        v_total_entradas::DECIMAL(8,2),
        v_total_salidas::DECIMAL(8,2),
        v_utilidad::DECIMAL(8,2),
        v_snapshot,
        false,
        CURRENT_TIMESTAMP,
        p_usuario_id
    )
    RETURNING reporte_id INTO p_nuevo_reporte_id;

    -- 8. Insertar partidas de Entradas desglosadas por canal
    IF v_tot_efectivo > 0 THEN
        INSERT INTO Entradas (reporte_id, tipo_entrada, monto, descripcion, marca_baja)
        VALUES (p_nuevo_reporte_id, 1, v_tot_efectivo::DECIMAL(8,2), 'Ventas en efectivo del período', false);
    END IF;

    IF v_tot_qr > 0 THEN
        INSERT INTO Entradas (reporte_id, tipo_entrada, monto, descripcion, marca_baja)
        VALUES (p_nuevo_reporte_id, 2, v_tot_qr::DECIMAL(8,2), 'Ventas por QR del período', false);
    END IF;

    IF v_tot_tarjeta > 0 THEN
        INSERT INTO Entradas (reporte_id, tipo_entrada, monto, descripcion, marca_baja)
        VALUES (p_nuevo_reporte_id, 3, v_tot_tarjeta::DECIMAL(8,2), 'Ventas con tarjeta del período', false);
    END IF;

    IF v_mov_entradas > 0 THEN
        INSERT INTO Entradas (reporte_id, tipo_entrada, monto, descripcion, marca_baja)
        VALUES (p_nuevo_reporte_id, 5, v_mov_entradas::DECIMAL(8,2), 'Movimientos de entrada del período', false);
    END IF;

    -- 9. Insertar partidas de Salidas desglosadas
    IF v_mov_salidas > 0 THEN
        INSERT INTO Salidas (reporte_id, tipo_salida, monto, descripcion, marca_baja)
        VALUES (p_nuevo_reporte_id, 5, v_mov_salidas::DECIMAL(8,2), 'Movimientos de salida del período', false);
    END IF;

END;
$$;
