-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ELIMINAR LOGICAMENTE UN MOVIMIENTO QUE AUN NO FUE SELLADO EN UN ARQUEO
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_movimientos_eliminar(
    p_movimiento_id INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Movimientos
    SET
        marca_baja           = true,
        fecha_modificacion   = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE movimiento_id = p_movimiento_id
      AND reporte_id IS NULL
      AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No se puede eliminar: el movimiento (ID: %) no existe o ya pertenece a un arqueo cerrado.', p_movimiento_id;
    END IF;
END;
$$;