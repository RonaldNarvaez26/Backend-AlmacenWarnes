-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     SOLICITAR EL CAMBIO DE CONTRASEÑA CAMBIANDO EL ESTADO DEL USUARIO
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_usuarios_solicitar_recuperacion(
    p_usuario VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Usuarios
    SET estado = 2,
        fecha_modificacion = CURRENT_TIMESTAMP
    WHERE usuario = p_usuario AND marca_baja = false;
END;
$$;