-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ACTUALIZAR CONTRASEÑA DE UN USUARIO (RESTABLECIMIENTO)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_usuarios_cambiar_password(
    p_usuario_id INT,
    p_nuevo_password_hash VARCHAR,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Usuarios
    SET 
        password = p_nuevo_password_hash,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id
      AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado o dado de baja (ID: %)', p_usuario_id;
    END IF;
END;
$$;