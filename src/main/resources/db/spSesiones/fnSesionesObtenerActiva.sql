-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     OBTENER LA SESIÓN ABIERTA ACTUAL DE UN USUARIO PARA CÁLCULO DE ARQUEO
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_sesiones_obtener_activa(
    p_usuario_id INT
)
RETURNS TABLE (
    sesion_id INT,
    usuario_id INT,
    hora_sesion_abierta TIMESTAMP,
    minutos_transcurridos INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.sesion_id,
        s.usuario_id,
        s.hora_sesion_abierta,
        GREATEST(
            0,
            ROUND(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - s.hora_sesion_abierta)) / 60.0)::INT
        ) AS minutos_transcurridos
    FROM Sesiones s
    WHERE s.usuario_id = p_usuario_id
      AND s.hora_sesion_cerrada IS NULL
      AND s.marca_baja = false
    ORDER BY s.hora_sesion_abierta DESC
    LIMIT 1;
END;
$$;