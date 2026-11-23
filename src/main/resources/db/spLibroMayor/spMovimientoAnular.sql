-- ============================================================================================
-- AUTOR:           Antigravity / Almacen Warnes
-- DESCRIPCION:     ANULACION / BAJA LOGICA DE MOVIMIENTOS O VENTAS EN EL LIBRO MAYOR
-- ============================================================================================
DROP PROCEDURE IF EXISTS sp_movimiento_anular(VARCHAR, INT, INT, VARCHAR, BOOLEAN, VARCHAR);
DROP PROCEDURE IF EXISTS sp_movimiento_anular(VARCHAR, INT, INT, VARCHAR);

CREATE OR REPLACE PROCEDURE sp_movimiento_anular(
    IN p_origen VARCHAR,           -- 'VENTA' o 'EXTRA_VENTA' / 'MOVIMIENTO'
    IN p_id INT,
    IN p_usuario_id INT,
    IN p_motivo VARCHAR,
    OUT p_exito BOOLEAN,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_actual INT;
    v_marca_baja BOOLEAN;
BEGIN
    p_exito := FALSE;

    IF p_id IS NULL OR p_id <= 0 THEN
        p_mensaje := 'Identificador no válido.';
        RETURN;
    END IF;

    IF UPPER(p_origen) = 'VENTA' THEN
        SELECT estado, marca_baja INTO v_estado_actual, v_marca_baja
        FROM Ventas
        WHERE venta_id = p_id;

        IF NOT FOUND THEN
            p_mensaje := 'Venta no encontrada (ID: ' || p_id || ').';
            RETURN;
        END IF;

        IF v_marca_baja = TRUE OR v_estado_actual = 3 THEN
            p_mensaje := 'La venta ya se encuentra anulada o dada de baja.';
            RETURN;
        END IF;

        UPDATE Ventas
        SET marca_baja = TRUE,
            estado = 3, -- 3 = Anulada
            anulado_por = p_usuario_id,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_id
        WHERE venta_id = p_id;

        UPDATE Detalles_Ventas
        SET marca_baja = TRUE
        WHERE venta_id = p_id;

        p_exito := TRUE;
        p_mensaje := 'Venta #' || p_id || ' anulada correctamente en el Libro Mayor.';

    ELSIF UPPER(p_origen) IN ('EXTRA_VENTA', 'MOVIMIENTO') THEN
        SELECT marca_baja INTO v_marca_baja
        FROM Movimientos
        WHERE movimiento_id = p_id;

        IF NOT FOUND THEN
            p_mensaje := 'Movimiento no encontrado (ID: ' || p_id || ').';
            RETURN;
        END IF;

        IF v_marca_baja = TRUE THEN
            p_mensaje := 'El movimiento ya se encuentra dado de baja.';
            RETURN;
        END IF;

        UPDATE Movimientos
        SET marca_baja = TRUE,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_id
        WHERE movimiento_id = p_id;

        p_exito := TRUE;
        p_mensaje := 'Movimiento #' || p_id || ' dado de baja correctamente en el Libro Mayor.';
    ELSE
        p_mensaje := 'Origen no reconocido (' || p_origen || '). Use VENTA o EXTRA_VENTA.';
    END IF;
END;
$$;
