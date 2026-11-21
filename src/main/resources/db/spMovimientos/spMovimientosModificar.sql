-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     MODIFICAR UN MOVIMIENTO DE CAJA ANTES DE QUE SEA SELLADO EN UN ARQUEO
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_movimientos_modificar(
    p_movimiento_id INT,
    p_monto DECIMAL(8,2),
    p_tipo_movimiento INT,
    p_metodo_movimiento INT,
    p_detalle VARCHAR(40),
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_monto IS NOT NULL AND p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del movimiento debe ser mayor a 0.';
    END IF;

    UPDATE Movimientos
    SET
        monto                = COALESCE(p_monto, monto),
        tipo_movimiento      = COALESCE(p_tipo_movimiento, tipo_movimiento),
        metodo_movimiento    = COALESCE(p_metodo_movimiento, metodo_movimiento),
        detalle              = COALESCE(TRIM(p_detalle), detalle),
        fecha_modificacion   = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE movimiento_id = p_movimiento_id
      AND reporte_id IS NULL
      AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Movimiento no encontrado, dado de baja o ya sellado en un arqueo (ID: %).', p_movimiento_id;
    END IF;
END;
$$;