-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     CERRAR SESIÓN ACTIVA AL FINALIZAR ARQUEO DE CAJA Y CALCULAR DURACIÓN EN MINUTOS
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sesiones_cerrar(
    p_sesion_id INT,
    p_usuario_id INT,
    INOUT p_duracion_minutos INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Sesiones
    SET
        hora_sesion_cerrada = CURRENT_TIMESTAMP,
        duracion_sesion = GREATEST(
            0,
            ROUND(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - hora_sesion_abierta)) / 60.0)::INT
        )
    WHERE sesion_id = p_sesion_id
      AND usuario_id = p_usuario_id
      AND hora_sesion_cerrada IS NULL
      AND marca_baja = false
    RETURNING duracion_sesion INTO p_duracion_minutos;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No se encontró una sesión abierta válida (ID: %) para el usuario %.', p_sesion_id, p_usuario_id;
    END IF;
END;
$$;