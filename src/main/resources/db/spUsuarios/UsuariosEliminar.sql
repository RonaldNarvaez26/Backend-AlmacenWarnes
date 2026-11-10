-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ELIMINAR LOGICAMENTE UN USUARIO DEL SISTEMA
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_usuarios_eliminar(
    p_usuario_id INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Usuarios
    SET 
        marca_baja = true,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id
      AND marca_baja = false;
END;
$$;