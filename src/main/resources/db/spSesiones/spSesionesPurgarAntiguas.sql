-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     PURGAR FISICAMENTE SESIONES CERRADAS MAYORES A N MESES (NUNCA ACTIVAS)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sesiones_purgar_antiguas(
    p_meses_retencion INT DEFAULT 3,
    INOUT p_registros_eliminados INT DEFAULT 0
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_meses_retencion IS NULL OR p_meses_retencion <= 0 THEN
        p_meses_retencion := 3;
    END IF;

    -- Borrar físicamente sesiones cerradas con más de p_meses_retencion de antigüedad
    -- NUNCA sesiones activas (hora_sesion_cerrada IS NULL)
    WITH eliminadas AS (
        DELETE FROM Sesiones
        WHERE hora_sesion_cerrada IS NOT NULL
          AND hora_sesion_cerrada < (CURRENT_TIMESTAMP - (p_meses_retencion || ' months')::INTERVAL)
        RETURNING sesion_id
    )
    SELECT COUNT(*)::INT INTO p_registros_eliminados FROM eliminadas;
END;
$$;
