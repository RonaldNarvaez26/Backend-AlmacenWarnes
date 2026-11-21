-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ABRIR SESIÓN TRAS LOGIN (O RETOMAR LA SESIÓN ACTIVA SI NO SE HIZO ARQUEO)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sesiones_abrir(
    p_usuario_id INT,
    INOUT p_sesion_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- 1. Verificar si el usuario ya tiene una sesión abierta pendiente de arqueo
    SELECT s.sesion_id INTO p_sesion_id
    FROM Sesiones s
    WHERE s.usuario_id = p_usuario_id
      AND s.hora_sesion_cerrada IS NULL
      AND s.marca_baja = false
    ORDER BY s.hora_sesion_abierta DESC
    LIMIT 1;

    -- Si ya tiene sesión abierta, la retornamos sin crear una nueva
    IF p_sesion_id IS NOT NULL THEN
        RETURN;
    END IF;

    -- 2. Si no tiene sesión abierta, creamos un nuevo registro de turno
    INSERT INTO Sesiones (
        usuario_id,
        hora_sesion_abierta,
        hora_sesion_cerrada,
        duracion_sesion,
        marca_baja
    )
    VALUES (
        p_usuario_id,
        CURRENT_TIMESTAMP,
        NULL,
        NULL,
        false
    )
    RETURNING sesion_id INTO p_sesion_id;
END;
$$;