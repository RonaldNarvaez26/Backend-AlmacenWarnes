-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez / Kenneth Lopez
-- DESCRIPCION:     MODIFICAR DATOS DEL USUARIO (SIN TOCAR LA CONTRASEÑA)
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_usuarios_modificar(INT, VARCHAR, VARCHAR, INT, INT);
DROP PROCEDURE IF EXISTS sp_usuarios_modificar(INT, INT, VARCHAR, VARCHAR, INT, INT);

CREATE OR REPLACE PROCEDURE sp_usuarios_modificar(
    p_usuario_id INT,
    p_sucursal_id INT,
    p_nombres VARCHAR,
    p_apellidos VARCHAR,
    p_rol_id INT,       
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE usuarios
    SET 
        sucursal_id = COALESCE(p_sucursal_id, 0),
        nombres = p_nombres,
        apellidos = p_apellidos,
        rol_id = p_rol_id,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado (ID: %)', p_usuario_id;
    END IF;
END;
$$;
