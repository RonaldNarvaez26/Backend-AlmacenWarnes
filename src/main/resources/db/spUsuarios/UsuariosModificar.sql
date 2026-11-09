-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
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
    UPDATE Usuarios
    SET 
        sucursal_id = COALESCE(p_sucursal_id, sucursal_id),
        nombres = COALESCE(p_nombres, nombres),
        apellidos = COALESCE(p_apellidos, apellidos),
        rol_id = COALESCE(p_rol_id, rol_id),
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id
      AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado o dado de baja (ID: %)', p_usuario_id;
    END IF;
END;
$$;
