-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     SOLICITAR EL CAMBIO DE CONTRASEÑA CAMBIANDO EL ESTADO DEL USUARIO
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_usuarios_solicitar_recuperacion(
    p_usuario VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado INT;
BEGIN
    SELECT estado
      INTO v_estado
      FROM Usuarios
     WHERE usuario = p_usuario AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No se encontró ningún usuario activo con el nombre: %', p_usuario;
    END IF;

    IF v_estado = 3 THEN
        RAISE EXCEPTION 'La cuenta de usuario se encuentra bloqueada. Contacte con un propietario o administrador.';
    END IF;

    UPDATE Usuarios
    SET estado = 2,
        fecha_modificacion = CURRENT_TIMESTAMP
    WHERE usuario = p_usuario AND marca_baja = false;
END;
$$;